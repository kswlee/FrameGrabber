FrameGrabber
============

An alternative solution to replace the getFrameAt method of Android MediaMetadataRetriever. FrameGrabber uses MediaCodec to decode video frame and use OpenGL to convert the video frame as RGB Bitmap. As Android MediaMetadataRetriever does not guarantee to return result when calling getFrameAtTime, this FrameGrabber can be used to extract video frame with frame accuracy.
