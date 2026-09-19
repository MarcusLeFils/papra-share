package app.paprashare.domain

import org.junit.Test
import org.junit.Assert

class PapraErrorMappingTest {

    @Test
    fun http400_isBadRequest() {
        Assert.assertEquals(PapraErrorKind.BAD_REQUEST, httpToErrorKind(400))
    }

    @Test
    fun http401_isUnauthorized() {
        Assert.assertEquals(PapraErrorKind.UNAUTHORIZED, httpToErrorKind(401))
    }

    @Test
    fun http403_isForbidden() {
        Assert.assertEquals(PapraErrorKind.FORBIDDEN, httpToErrorKind(403))
    }

    @Test
    fun http404_isNotFound() {
        Assert.assertEquals(PapraErrorKind.NOT_FOUND, httpToErrorKind(404))
    }

    @Test
    fun http409_isDuplicate() {
        Assert.assertEquals(PapraErrorKind.DUPLICATE, httpToErrorKind(409))
    }

    @Test
    fun http413_isTooLarge() {
        Assert.assertEquals(PapraErrorKind.TOO_LARGE, httpToErrorKind(413))
    }

    @Test
    fun http429_isRateLimited() {
        Assert.assertEquals(PapraErrorKind.RATE_LIMITED, httpToErrorKind(429))
    }

    @Test
    fun http5xx_isServerError() {
        Assert.assertEquals(PapraErrorKind.SERVER_ERROR, httpToErrorKind(500))
        Assert.assertEquals(PapraErrorKind.SERVER_ERROR, httpToErrorKind(503))
        Assert.assertEquals(PapraErrorKind.SERVER_ERROR, httpToErrorKind(599))
    }

    @Test
    fun httpUnknown_fallsBackToHttp() {
        Assert.assertEquals(PapraErrorKind.HTTP, httpToErrorKind(200))
        Assert.assertEquals(PapraErrorKind.HTTP, httpToErrorKind(302))
        Assert.assertEquals(PapraErrorKind.HTTP, httpToErrorKind(422))
        Assert.assertEquals(PapraErrorKind.HTTP, httpToErrorKind(600))
    }
}