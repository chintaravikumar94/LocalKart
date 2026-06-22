package com.localkart.common.repo

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Uploads images to Firebase Storage and returns their public download URL. */
object StorageRepo {
    suspend fun uploadImage(uri: Uri, folder: String): String {
        val path = "$folder/${UUID.randomUUID()}.jpg"
        val ref = Firebase.storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
