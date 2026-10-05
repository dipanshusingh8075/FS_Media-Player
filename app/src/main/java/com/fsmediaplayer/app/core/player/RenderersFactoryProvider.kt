package com.fsmediaplayer.app.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.fsmediaplayer.app.core.model.DecoderMode

@OptIn(UnstableApi::class)
object RenderersFactoryProvider {

    /**
     * Builds a RenderersFactory configured for Hardware or Software decoding.
     */
    fun buildRenderersFactory(
        context: Context,
        decoderMode: DecoderMode
    ): RenderersFactory {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        return when (decoderMode) {
            DecoderMode.HARDWARE -> {
                // MediaCodecSelector.DEFAULT prefers hardware decoders if available
                renderersFactory.setMediaCodecSelector(MediaCodecSelector.DEFAULT)
            }
            DecoderMode.SOFTWARE -> {
                // Software decoding selector: prioritize software-only codecs (c2.android.* or omx.google.*)
                renderersFactory.setMediaCodecSelector(object : MediaCodecSelector {
                    override fun getDecoderInfos(
                        mimeType: String,
                        requiresSecureDecoder: Boolean,
                        requiresTunnelingDecoder: Boolean
                    ): List<MediaCodecInfo> {
                        val allDecoders = MediaCodecUtil.getDecoderInfos(
                            mimeType,
                            requiresSecureDecoder,
                            requiresTunnelingDecoder
                        )

                        // Sort software decoders to the front of the list
                        return allDecoders.sortedWith(
                            compareByDescending<MediaCodecInfo> { it.softwareOnly }
                                .thenByDescending {
                                    it.name.startsWith("c2.android") || it.name.startsWith("omx.google")
                                }
                        )
                    }
                })
            }
        }
    }
}
