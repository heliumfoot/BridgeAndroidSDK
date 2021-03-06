/*
 * BSD 3-Clause License
 *
 * Copyright 2018  Sage Bionetworks. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1.  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3.  Neither the name of the copyright holder(s) nor the names of any contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission. No license is granted to the trademarks of
 * the copyright holders even if such marks are included in this software.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sagebionetworks.research.sageresearch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.reflect.TypeToken
import io.reactivex.Flowable
import junit.framework.Assert.assertNull
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.sagebionetworks.bridge.rest.model.ForwardCursorReportDataList
import org.sagebionetworks.bridge.rest.model.ReportDataList
import org.sagebionetworks.bridge.rest.model.ScheduledActivityListV4
import org.sagebionetworks.research.sageresearch.dao.room.*
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
abstract class RoomTestHelper {

    companion object {
        lateinit var database: ResearchDatabase
        lateinit var activityDao: ScheduledActivityEntityDao
        lateinit var reportDao: ReportEntityDao
        lateinit var resourceDao: ResourceEntityDao
        lateinit var historyDao: HistoryItemEntityDao

        @BeforeClass
        @JvmStatic fun setup() {
            database = Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), ResearchDatabase::class.java)
                    .allowMainThreadQueries().build()

            activityDao = database.scheduleDao()
            reportDao = database.reportDao()
            resourceDao = database.resourceDao()
            historyDao = database.historyDao()
        }

        @AfterClass
        @JvmStatic fun teardown() {
            database.close()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(InterruptedException::class)
    fun <T> getValue(liveData: LiveData<T>): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                data[0] = o
                latch.countDown()
                liveData.removeObserver(this)
            }
        }
        liveData.observeForever(observer)
        latch.await(1, TimeUnit.SECONDS)

        return data[0] as T
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(InterruptedException::class)
    fun <T> getValue(flowable: Flowable<List<T>>): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        flowable.subscribe {
            if (!it.isEmpty()) {
                data[0] = it.get(0)
            }
            latch.countDown()
        }
        latch.await(1, TimeUnit.SECONDS)

        return data[0] as T
    }
}

object TestResourceHelper {

    val entityTypeConverters = EntityTypeConverters()

    fun testResourceMap(resourceSet: Set<String>): Map<String, List<ScheduledActivityEntity>> {
        return resourceSet.associateBy( {it}, {
            testResource(it)
        } )
    }

    internal fun testResource(filename: String): List<ScheduledActivityEntity> {
        val json = testResourceJson(filename)
        val testList = entityTypeConverters.bridgeGson.fromJson(json, ScheduledActivityListV4::class.java)
        return EntityTypeConverters().fromScheduledActivityListV4(testList) ?: ArrayList()
    }

    fun testResourceForwardCursorReportDataList(filename: String): ForwardCursorReportDataList {
        val json = testResourceJson(filename)
        return entityTypeConverters.bridgeGson.fromJson(json, ForwardCursorReportDataList::class.java)
    }

    fun testResourceReportDataList(filename: String): ReportDataList {
        val json = testResourceJson(filename)
        return entityTypeConverters.bridgeGson.fromJson(json, ReportDataList::class.java)
    }

    fun testResourceReportEntityList(filename: String): List<ReportEntity> {
        val json = testResourceJson(filename)
        val listType = object : TypeToken<List<ReportEntity>>() {}.type
        return entityTypeConverters.bridgeGson.fromJson(json, listType)
    }

    // This surpress was auto-generated by AS for getting a class' classLoader
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    internal fun testResourceJson(filename: String): String {
        var json: String? = null
        try {
            val inputStream = RoomTestHelper::class.java.classLoader.getResourceAsStream(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.defaultCharset())
        } catch (e: IOException) {
            assertNull("Error loading class resource", e)
        }
        return json ?: ""
    }
}