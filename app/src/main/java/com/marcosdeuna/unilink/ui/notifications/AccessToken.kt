package com.marcosdeuna.unilink.ui.notifications

import com.google.auth.oauth2.GoogleCredentials
import java.io.InputStream

class AccessToken {

    val firebaseMessagingScope: String = "https://www.googleapis.com/auth/firebase.messaging"

    fun getAccessToken(): String {
        try{

            val jsonString: String = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"unilink-fe270\",\n" +
                    "  \"private_key_id\": \"2663aebfc62a07e7bfcb6e3e79684b3e09019ffb\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC6fUd0oBjm7fbq\nHXKC97ygLG63gWCsWXS2xsSd+qqa9bGMaJlK9GeGTLI7Xt+M4OIXO+iAsfREvg9K\nrEHCsuRA+Ql8Gm8hGxRHy/EhOcOpX5iH3gmXc+wRzU4jXTgGi44wUdw2cPA3dvTA\n61lSN1nj3WFaBeC9LzoA7lSEUWAk/ofzmXYX+rqtpLSxCpfmx1bLnuSsnTRRedtl\ndgrkkrT0VdifDmLZSPScbzJTQ3E3C1Df51hZRkZ4ctbNpFGEsENrSnrtSsmNWQyu\ntFRckAbwQW0J/eToSPQr5ZX3X8tE6lU1IArTJ7EKw7HgEQ1++sNFSJW/tIxE33L6\nk3gQGE5PAgMBAAECggEAAZFMZC/gO0q/8bMCIhsLLsPRl9O1do3JM4JYlvwbrW8s\nkswszJPv3XDyZCuB2OWuANe29PpChK5uWkn2h0RxISdWAJvwFoyMGDXYgradfvwv\nqCLar+zlnopc7Zh5U58tJrSAhNAq3m06q4ABjKsXduZpBU/0XAf0MzBodjUEESHa\nE+B6nh+2Cn2xT+c3Bo+SjASVFo6Azc9c1JVeY9O6jg7n3WxHhSrDvY33+OI+c+zb\nJpYIPN25luMUxgk+0ByuUspW2XAynn2bnz1y6WTyusdotPs7mDdqqOjivY1zQD+F\ncwrPqCnMCYV53xhaMqKjhxEHIpn7yWu+kHR3Ri31AQKBgQDibQ6T1LtRF7irh2+C\nWhJD7EfBN+KtV8xQ3ZaVvx30a+eZ/M1HmCD+KDGnmBRyM6UNRLRyrTxCfJdzVpVt\nMmzxpR4VVbTbpnUbVIzb/wGj4vRA4XOylEAThAPy2Fu/ctbxrIB3lVTen3FUbtYo\nCD2m4qRCD3xJ3sgDC2QPjM/zkQKBgQDS2N9GKjeC7ojq5VGpNvzsdalzWoGA0i0M\ni1jCOrDmb3UWwQJgLjKbho2gck7GXreH81qyAczqlWVnAmWaX/E4ba3W6Uxe5+iq\nzEyC7pw0qi25jPzqyAtYVXuQZN8Dz+xBkzkUUsMfjaI7J+sZw/IDCfswBig8Uio0\nk0GajXlz3wKBgAX4WioLseSxe20KZSZKubCQylON5awsZHa9YPsRzvhi3/hvcfox\nSK1q4eRJXkCkm/V9mkRrzOrz3gpsfgQjEDxO3jwxxlMEUJIn6I7kaman1UjgqOxM\nfGQHVxNQIRsrK/+zTK/agCJekd5oDb2Aeh+sWihjwCMyfJOX+UtvqwwBAoGAIob5\nKRhIttww96ZN1RP4HxC1ivpLMrk4P1GiW0wZI+MknXFF3lIyX12NW7TcSYfBLjYf\np/67e76zOEcgNEN50O/FA+h4ZSI865tA/D/uvIERs8zurdPMZ863yVF7Y7hsy+A7\n/KQA5+3dnypOSY3Y176Or7KSbC5YUvu6RoDcXqkCgYAfSIuBdxBE9NbH47wZF3M9\ngr7uT7Z/3UzhJ9r6PCVDpAL54cq/6RqBSWHG9cVgK6eU7zANJvlMyqiTptb7Xsw7\n7lG5w2fHHxWDt8sAO8fpYIQH2KruD28PE/Z1YXt8jnuIU+v1lUTPkNiM88Lg8Zil\nJhfWuGWUyE/IxYQCZGXtuw==\n-----END PRIVATE KEY-----\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-u1ls0@unilink-fe270.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"109124101558994751151\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-u1ls0%40unilink-fe270.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}"
            val jsonStream: InputStream = jsonString.byteInputStream()

            val googleCredentials = GoogleCredentials.fromStream(jsonStream).createScoped(
                listOf(firebaseMessagingScope)
            )

            googleCredentials.refresh()

            return googleCredentials.accessToken.tokenValue

        }catch (e: Exception) {
            throw Exception(
                "Error al obtener el token de acceso: ${e.message}"
            )
        }
    }
}