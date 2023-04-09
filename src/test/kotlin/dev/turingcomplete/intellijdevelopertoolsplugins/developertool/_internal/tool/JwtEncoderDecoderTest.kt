package dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal.tool

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder
import io.ktor.util.*
import org.skyscreamer.jsonassert.JSONAssert

internal class JwtEncoderDecoderTest : DeveloperToolTestBase<JwtEncoderDecoder>(factory = JwtEncoderDecoder.Factory()) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun test() {
    val encodedJwt = JWT.create()
      .withIssuer("my-issuer")
      .withAudience("my-audience")
      .withClaim("my-claim", "my-claim-value")
      .sign(Algorithm.HMAC256("s3cre!"))

    uiInput {
      developerTool.encodedEditor.text = encodedJwt
    }

    val actualHeaderJson = developerTool.headerEditor.text
    JSONAssert.assertEquals(JWT.decode(encodedJwt).header.decodeBase64String(), actualHeaderJson, true)

    val actualPayloadJson = developerTool.payloadEditor.text
    JSONAssert.assertEquals(JWT.decode(encodedJwt).payload.decodeBase64String(), actualPayloadJson, true)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}