package events.boudicca.search.query

class Parser(private val tokens: List<Token>) {
    private var i = 0
    private var groupDepth = 0
    private var lastExpression: Expression? = null
    fun parse(): Expression {
        parseExpression()
        if (groupDepth > 0) {
            throw IllegalStateException("not all groups are closed!")
        }
        return lastExpression ?: throw IllegalStateException("could not parse any expressions?")
    }

    private fun parseExpression() {
        if (i == tokens.size) {
            throw IllegalStateException("expecting expression but encountered end of tokens")
        }
        val token = tokens[i]
        if (token.getType() == TokenType.TEXT) {
            parseTextExpression()
        } else if (token.getType() == TokenType.NOT) {
            parseNotExpression()
        } else if (token.getType() == TokenType.GROUPING_OPEN) {
            parseGroupOpen()
        } else {
            throw IllegalStateException("unexpected token ${token.getType()} at start of expression at index $i")
        }
        if (i != tokens.size) {
            val token = tokens[i]
            if (token.getType() == TokenType.AND || token.getType() == TokenType.OR) {
                parseBooleanExpression()
            } else if (token.getType() == TokenType.GROUPING_CLOSE) {
                parseGroupClosed()
            } else {
                throw IllegalStateException("unexpected token ${token.getType()} after end of expression at index $i")
            }
        }
    }

    private fun parseGroupOpen() {
        groupDepth++
        i++
        parseExpression()
    }

    private fun parseGroupClosed() {
        groupDepth--
        if (groupDepth < 0) {
            throw IllegalStateException("closing non-existing group at index $i")
        }
        i++
    }

    private fun parseBooleanExpression() {
        val token = tokens[i]
        val savedLastExpression = this.lastExpression!!
        i++
        parseExpression()
        if (token.getType() == TokenType.AND) {
            lastExpression = AndExpression(savedLastExpression, lastExpression!!)
        } else if (token.getType() == TokenType.OR) {
            lastExpression = OrExpression(savedLastExpression, lastExpression!!)
        } else {
            throw IllegalStateException("unknown token type ${token.getType()}")
        }
    }

    private fun parseNotExpression() {
        i++
        parseExpression()
        lastExpression = NotExpression(lastExpression!!)
    }

    private fun parseTextExpression() {
        if (i + 2 >= tokens.size) {
            throw IllegalStateException("trying to parse text expression and needing 3 arguments, but there are not enough at index $i")
        }
        val fieldName = getText(i)
        val expression = getTextExpression(i + 1)
        val text = getText(i + 2)

        if (expression.getType() == TokenType.CONTAINS) {
            lastExpression = ContainsExpression(fieldName.getToken()!!, text.getToken()!!)
        } else if (expression.getType() == TokenType.EQUALS) {
            lastExpression = EqualsExpression(fieldName.getToken()!!, text.getToken()!!)
        } else {
            throw IllegalStateException("unknown token type ${expression.getType()}")
        }
        i += 3
    }

    private fun getTextExpression(i: Int): Token {
        val token = tokens[i]
        if (token.getType() != TokenType.CONTAINS && token.getType() != TokenType.EQUALS) {
            throw IllegalStateException("expecting text expression token at index $i but was ${token.getType()}")
        }
        return token
    }

    private fun getText(i: Int): Token {
        val token = tokens[i]
        if (token.getType() != TokenType.TEXT) {
            throw IllegalStateException("expecting text token at index $i but was ${token.getType()}")
        }
        return token
    }
}
