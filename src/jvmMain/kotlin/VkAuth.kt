import com.vk.api.sdk.client.TransportClient
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.photos.responses.GetResponse

class VkAuth {
    private var vk: VkApiClient
    private var actor: UserActor? = null
    var token: String? = null
    var userId: Int? = null

    init {
        val transportClient: TransportClient = HttpTransportClient.getInstance()
        vk = VkApiClient(transportClient)
    }

    fun oAuth() {
        actor = UserActor(userId, token)
    }

    fun getPhotos(): GetResponse {
        return vk.photos().get(actor)

            .count(10)
            .offset(5)
            .execute()
    }


}