package org.sagebionetworks.research.sageresearch.dao.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.sagebionetworks.bridge.rest.RestUtils
import java.lang.reflect.Type

@Entity(primaryKeys = arrayOf("identifier", "type"))
data class ResourceEntity(

    /**
     * @property identifier the resource identifier
     */
    var identifier: String,
    /**
     * @property type the type of data stored in this resource
     */
    @ColumnInfo(index = true)
    var type: ResourceType,
    /**
     * @property resourceJson JSON string representing the resource being stored
     */
    var resourceJson: String? = null,
    /**
     * @property lastUpdateTime time in milliseconds that Resource was last updated from server
     */
    var lastUpdateTime: Long

    ) {

    @Transient
    private var resource: Any? = null

    enum class ResourceType() {
        SURVEY(),
        APP_CONFIG()
    }

    fun <T> loadResource(classOfT: Class<T>): T {
        if (resource == null) {
            resource = RestUtils.GSON.fromJson(resourceJson, classOfT)
        }
        return resource as T
    }



}