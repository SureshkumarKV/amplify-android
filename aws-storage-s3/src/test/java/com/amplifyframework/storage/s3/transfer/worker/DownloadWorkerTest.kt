/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.storage.s3.transfer.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.readChannel
import aws.smithy.kotlin.runtime.util.InternalApi
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.transfer.TransferDB
import com.amplifyframework.storage.s3.transfer.TransferRecord
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class DownloadWorkerTest {
    private lateinit var context: Context
    private lateinit var s3Client: S3Client
    private lateinit var transferDB: TransferDB
    private lateinit var transferStatusUpdater: TransferStatusUpdater
    private lateinit var workerParameters: WorkerParameters

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        workerParameters = mockk(WorkerParameters::class.java.name)
        s3Client = mockk<S3Client>(relaxed = true, relaxUnitFun = true)
        transferDB = mockk(TransferDB::class.java.name)
        transferStatusUpdater = mockk(TransferStatusUpdater::class.java.name)
        every { workerParameters.inputData }.answers { workDataOf(BaseTransferWorker.TRANSFER_RECORD_ID to 1) }
        every { workerParameters.runAttemptCount }.answers { 1 }
        every { workerParameters.taskExecutor }.answers { ImmediateTaskExecutor() }
    }

    @Test
    fun testDownloadOnSuccess() = runTest {
        val transferRecord = TransferRecord(
            1,
            UUID.randomUUID().toString(),
            bucketName = "bucket_name",
            key = "key",
            file = File.createTempFile("Temp", "_file.txt").path
        )

        val response = GetObjectResponse {
            contentLength = 10
            body = ByteStream.readAsOneShotStream(createFile(10))
        }
        coEvery {
            s3Client.getObject(
                any(),
                any<suspend (GetObjectResponse) -> ListenableWorker.Result>()
            )
        }.coAnswers { secondArg<suspend (GetObjectResponse) -> ListenableWorker.Result>().invoke(response) }
        every { transferDB.getTransferRecordById(any()) }.answers { transferRecord }
        every { transferStatusUpdater.updateProgress(1, any(), any(), true, false) }.answers { }
        every { transferStatusUpdater.updateProgress(1, any(), any(), true, true) }.answers { }

        val worker = DownloadWorker(s3Client, transferDB, transferStatusUpdater, context, workerParameters)
        val result = worker.doWork()

        verify(atLeast = 1) { transferStatusUpdater.updateProgress(1, 10 * 1024 * 1024, 10 * 1024 * 1024, true, true) }
        val expectedResult =
            ListenableWorker.Result.success(workDataOf(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID to 1))
        assertEquals(expectedResult, result)
    }

    @Test
    fun testDownloadOnError() = runTest {
        val transferRecord = TransferRecord(
            1,
            UUID.randomUUID().toString(),
            bucketName = "bucket_name",
            key = "key",
            file = File.createTempFile("Temp", "_file.txt").path
        )
        coEvery {
            s3Client.getObject(
                any(),
                any<suspend (GetObjectResponse) -> ListenableWorker.Result>()
            )
        }.coAnswers { throw IllegalArgumentException() }
        every { transferDB.getTransferRecordById(any()) }.answers { transferRecord }
        every { transferStatusUpdater.updateProgress(1, any(), any(), true) }.answers { }
        every { transferStatusUpdater.updateTransferState(1, TransferState.FAILED) }.answers { }
        every { transferStatusUpdater.updateOnError(1, any()) }.answers { }

        val worker = DownloadWorker(s3Client, transferDB, transferStatusUpdater, context, workerParameters)
        val result = worker.doWork()

        verify(exactly = 0) {
            transferStatusUpdater.updateProgress(
                1,
                1000,
                1000,
                notifyListener = true,
                updateDB = true
            )
        }
        val expectedResult =
            ListenableWorker.Result.failure(workDataOf(BaseTransferWorker.OUTPUT_TRANSFER_RECORD_ID to 1))
        assertEquals(expectedResult, result)
    }

    private fun createFile(size: Int): File {
        val file = File((System.getProperty("java.io.tmpdir")?.plus(File.separator)) + "file")
        file.createNewFile()
        val raf = RandomAccessFile(file, "rw")
        raf.setLength((size * 1024 * 1024).toLong())
        raf.close()
        file.deleteOnExit()
        return file
    }

    @OptIn(InternalApi::class)
    private fun ByteStream.Companion.readAsOneShotStream(
        file: File,
        start: Long = 0,
        length: Long = file.length(),
    ): ByteStream {
        return object : ByteStream.OneShotStream() {
            override val contentLength: Long = length
            override fun readFrom(): SdkByteReadChannel {
                val oneShotStream = file.readChannel(start, start + length - 1)
                return object : SdkByteReadChannel {
                    override val availableForRead: Int
                        get() = oneShotStream.availableForRead
                    override val isClosedForRead: Boolean
                        get() = oneShotStream.isClosedForRead
                    override val isClosedForWrite: Boolean
                        get() = oneShotStream.isClosedForWrite

                    override suspend fun awaitContent() {
                        oneShotStream.awaitContent()
                    }

                    override fun cancel(cause: Throwable?): Boolean {
                        return oneShotStream.cancel(cause)
                    }

                    override suspend fun readAvailable(sink: ByteBuffer): Int {
                        return oneShotStream.readAvailable(sink)
                    }

                    override suspend fun readAvailable(sink: ByteArray, offset: Int, length: Int): Int {
                        return oneShotStream.readAvailable(sink, offset, length)
                    }

                    override suspend fun readFully(sink: ByteArray, offset: Int, length: Int) {
                        return oneShotStream.readFully(sink, offset, length)
                    }

                    override suspend fun readRemaining(limit: Int): ByteArray {
                        return readRemaining(limit)
                    }
                }
            }
        }
    }
}
