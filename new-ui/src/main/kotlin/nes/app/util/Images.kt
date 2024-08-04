package nes.app.util

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class Images @Inject constructor(
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context
) {

    private val images = listOf(
        // add some random images to display
        // Images used with permission (c) Jason Guss, James Bryan, Mike Rambo, Evan Krohn
        "https://i.imgur.com/qhqUJWh.jpg",
        "https://i.imgur.com/uvxpvkQ.jpg",
        "https://i.imgur.com/AHls6H6.jpg",
        "https://i.imgur.com/uxJVage.jpg",
        "https://i.imgur.com/u4ShCFi.jpg",
        "https://i.imgur.com/Fywi5vG.jpg",
        "https://i.imgur.com/aZrqwDT.jpg",
        "https://i.imgur.com/AECeOue.jpg",
        "https://i.imgur.com/MHGKZqL.jpg",
        "https://i.imgur.com/Oi3DnPt.jpg",
        "https://i.imgur.com/FsfazYF.jpg",
        "https://i.imgur.com/LAT9Q16.jpg",
        "https://i.imgur.com/bhkGDLT.jpg",

        // Images used with permission (c) Andrea Nusinov, AZN Photography
        // www.instagram.com/aznpics

        "https://i.imgur.com/XAp4BUA.jpg",
        "https://i.imgur.com/9qRlnMl.jpg",
        "https://i.imgur.com/LTTNpG6.jpg",
        "https://i.imgur.com/Iv9EGzr.jpg",
        "https://i.imgur.com/MYuqi0Z.jpg",
        "https://i.imgur.com/PQfiuPy.jpg",
        "https://i.imgur.com/YBRbMYv.jpg",
        "https://i.imgur.com/GJyOac0.jpg",
        "https://i.imgur.com/v3wsHr0.jpg",
        "https://i.imgur.com/WzcS3Sc.jpg",
        "https://i.imgur.com/ej1eiy1.jpg",
        "https://i.imgur.com/ZYA1ilw.jpg",
        "https://i.imgur.com/Fi9TXU1.jpg",
        "https://i.imgur.com/2WN1PBC.jpg",
        "https://i.imgur.com/ZukejZd.jpg",
        "https://i.imgur.com/uPohYjt.jpg",
        "https://i.imgur.com/MEYqCKP.jpg",
        "https://i.imgur.com/2UnuEMB.jpg",
        "https://i.imgur.com/TanyMAh.jpg",
        "https://i.imgur.com/sSCeoR3.jpg",
        "https://i.imgur.com/YYLy4hd.jpg",
        "https://i.imgur.com/84i9wEt.jpg",
        "https://i.imgur.com/p3My2pv.jpg",
        "https://i.imgur.com/6aG3Wdr.jpg",
        "https://i.imgur.com/owGLpm2.jpg",
        "https://i.imgur.com/1HN6ifl.jpg",
        "https://i.imgur.com/6QPf69F.jpg",
        "https://i.imgur.com/TX1Aok3.jpg",
        "https://i.imgur.com/ha1doJI.jpg",

        // Images used with permission (c) David Logan, David Logan Photography
        // https://www.flickr.com/photos/davidloganphotography
        "https://c2.staticflickr.com/8/7428/9407135018_df133c9c28_b.jpg",
        "https://c2.staticflickr.com/6/5344/9407138068_2d4f876588_b.jpg",
        "https://c1.staticflickr.com/3/2827/9407148598_3b53cc5e1b_b.jpg",
        "https://c2.staticflickr.com/4/3805/9404388499_6b446a4e0c_b.jpg",
        "https://c2.staticflickr.com/6/5327/9407144032_01fb41745c_b.jpg",
        "https://c2.staticflickr.com/8/7365/9404384233_75eebb6e76_b.jpg",
        "https://c1.staticflickr.com/9/8229/8394959099_d4b1d30017_b.jpg",
        "https://c1.staticflickr.com/9/8361/8394958861_af3d802945_b.jpg",
        "https://c1.staticflickr.com/9/8189/8394665046_0c6a426395_b.jpg",
        "https://c1.staticflickr.com/3/2478/3654332535_c3826a066b_b.jpg",
        "https://c2.staticflickr.com/4/3359/3655135680_276263f359_b.jpg",
        "https://c1.staticflickr.com/3/2443/3655134046_d9f4697a4a_b.jpg",
        "https://c1.staticflickr.com/3/2776/4151295029_abc36c90c4_b.jpg"
    )

    init {
        // prefetch all images
        images.forEach {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(it)
                    .build()
                imageLoader.enqueue(request)
            }.onFailure {
                Timber.e(it, "Error loading an image")
            }
        }
    }

    val randomImageUrl get() = images.random()
}
