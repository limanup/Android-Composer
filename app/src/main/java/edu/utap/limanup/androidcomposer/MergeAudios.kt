package edu.utap.limanup.androidcomposer

import android.content.Context
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

// https://stackoverflow.com/questions/23112372/android-concat-2-audio-wav-or-mp3-files
// http://soundfile.sapp.org/doc/WaveFormat/
class MergeAudios {
    private fun createWavHeader(outputFile: String, sampleRate: Long): ByteArray {
        val numChannels: Long = 2
        val bitsPerSample: Long = 16
        var dataSize: Long = 0
        try {
            val fileSize = FileInputStream(outputFile)
            dataSize = fileSize.channel.size()
            fileSize.close()
        } catch (e1: FileNotFoundException) {
            // TODO Auto-generated catch block
            e1.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        val fileSize = dataSize + 36
        val byteRate: Long = bitsPerSample * sampleRate * numChannels / 8
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (fileSize and 0xff).toByte() // file size
        header[5] = (fileSize shr 8 and 0xff).toByte()
        header[6] = (fileSize shr 16 and 0xff).toByte()
        header[7] = (fileSize shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk. 16 for PCM.
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1, PCM = 1
        header[21] = 0
        header[22] = numChannels.toByte() // Mono = 1, Stereo = 2, etc.
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte() // sample rate chunk
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte() // byte rate chunk
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (bitsPerSample * numChannels / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // start of data subchunk
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte() // data size
        header[41] = (dataSize shr 8 and 0xff).toByte()
        header[42] = (dataSize shr 16 and 0xff).toByte()
        header[43] = (dataSize shr 24 and 0xff).toByte()

        return header
    }



    fun localMerge(context: Context, localFiles: List<Int>, outputFile: String) {
        var sampleRate: Long = 0
        try {
            val fos = DataOutputStream(
                BufferedOutputStream(FileOutputStream(outputFile))
            )
            val fis = mutableListOf<DataInputStream>()
            val sizes = LongArray(localFiles.size)
            for (i in localFiles.indices) {
                val localIS = context.resources.openRawResource(localFiles[i])
                sizes[i] = (localIS.available().toLong() - 44) / 2
            }
            for (i in localFiles.indices) {
                fis.add(
                    i, DataInputStream(
                        BufferedInputStream(context.resources.openRawResource(localFiles[i]))
                    )
                )
                if (i == localFiles.size - 1) {
                    fis[i].skip(24)
                    val sampleRt = ByteArray(4)
                    fis[i].read(sampleRt)
                    val bbInt: ByteBuffer = ByteBuffer.wrap(sampleRt).order(ByteOrder.LITTLE_ENDIAN)
                    sampleRate = bbInt.int.toLong()
                    fis[i].skip(16)
                } else {
                    fis[i].skip(44)
                }
            }
            for (b in localFiles.indices) {
                for (i in 0 until sizes[b].toInt()) {
                    val dataBytes = ByteArray(2)
                    try {
                        dataBytes[0] = fis[b].readByte()
                        dataBytes[1] = fis[b].readByte()
                    } catch (e: EOFException) {
                        fos.close()
                    }
                    val dataInShort: Short =
                        ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN).short
                    val dataInFloat = dataInShort.toFloat() / 37268.0f
                    val outputSample = (dataInFloat * 37268.0f).toInt().toLong()
                    val dataFin = ByteArray(2)
                    dataFin[0] = (outputSample and 0xff).toByte()
                    dataFin[1] = (outputSample shr 8 and 0xff).toByte()
                    fos.write(dataFin, 0, 2)
                }
            }
            fos.close()
            for (i in localFiles.indices) {
                fis[i].close()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val header = createWavHeader(outputFile, sampleRate)
        try {
            val rFile = RandomAccessFile(outputFile, "rw")
            rFile.seek(0)
            rFile.write(header)
            rFile.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }


    fun mergeWavFiles(wavFiles: List<String>, outputFile: String) {
        var sampleRate: Long = 0
        try {
            val fos = DataOutputStream(
                BufferedOutputStream(FileOutputStream(outputFile))
            )
            val fis = mutableListOf<DataInputStream>()
            val sizes = LongArray(wavFiles.size)
            for (i in wavFiles.indices) {
                val file = File(wavFiles[i])
                sizes[i] = (file.length() - 44) / 2
            }
            for (i in wavFiles.indices) {
                fis.add(
                    i, DataInputStream(
                        BufferedInputStream(FileInputStream(wavFiles[i]))
                    )
                )
                if (i == wavFiles.size - 1) {
                    fis[i].skip(24)
                    val sampleRt = ByteArray(4)
                    fis[i].read(sampleRt)
                    val bbInt: ByteBuffer = ByteBuffer.wrap(sampleRt).order(ByteOrder.LITTLE_ENDIAN)
                    sampleRate = bbInt.int.toLong()
                    fis[i].skip(16)
                } else {
                    fis[i].skip(44)
                }
            }
            for (b in wavFiles.indices) {
                for (i in 0 until sizes[b].toInt()) {
                    val dataBytes = ByteArray(2)
                    try {
                        dataBytes[0] = fis[b].readByte()
                        dataBytes[1] = fis[b].readByte()
                    } catch (e: EOFException) {
                        fos.close()
                    }
                    val dataInShort: Short =
                        ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN).short
                    val dataInFloat = dataInShort.toFloat() / 37268.0f
                    val outputSample = (dataInFloat * 37268.0f).toInt().toLong()
                    val dataFin = ByteArray(2)
                    dataFin[0] = (outputSample and 0xff).toByte()
                    dataFin[1] = (outputSample shr 8 and 0xff).toByte()
                    fos.write(dataFin, 0, 2)
                }
            }
            fos.close()
            for (i in wavFiles.indices) {
                fis[i].close()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val header = createWavHeader(outputFile, sampleRate)
        try {
            val rFile = RandomAccessFile(outputFile, "rw")
            rFile.seek(0)
            rFile.write(header)
            rFile.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }


}