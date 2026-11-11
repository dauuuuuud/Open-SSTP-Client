package kittoku.osc.app.util

object ErrorMessages {
    fun friendlyMessage(rawError: String?): String {
        if (rawError.isNullOrBlank()) return "The connection failed."

        return when {
            "ERR_TIMEOUT" in rawError -> "The server did not respond in time. Try another server or check your network."
            "ERR_VERIFICATION_FAILED" in rawError -> "The server certificate could not be verified."
            "ERR_AUTHENTICATION_FAILED" in rawError -> "Authentication failed. Check the username and password."
            "ERR_NEGATIVE_ACKNOWLEDGED" in rawError -> "The server refused the SSTP call."
            "ERR_DISCONNECT_REQUESTED" in rawError -> "The server requested to end the session."
            "ERR_ABORT_REQUESTED" in rawError -> "The server aborted the SSTP call."
            "ERR_COUNT_EXHAUSTED" in rawError -> "The server stopped responding to SSTP requests."
            "ERR_ADDRESS_REJECTED" in rawError -> "The server rejected the requested IP address."
            "ERR_INVALID_ADDRESS" in rawError -> "The server did not assign a valid IP address."
            "ERR_PARSING_FAILED" in rawError -> "Received malformed data from the server."
            "ERR_PROTOCOL_REJECTED" in rawError -> "The server rejected a required network protocol."
            "ERR_UNKNOWN_TYPE" in rawError -> "Received an unknown message type from the server."
            "ERR_UNEXPECTED_MESSAGE" in rawError -> "Received an unexpected message from the server."
            "ERR_TERMINATE_REQUESTED" in rawError -> "The PPP session was terminated by the server."
            else -> "The connection failed ($rawError)."
        }
    }
}
