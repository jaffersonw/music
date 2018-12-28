package com.goodjob.musicplayer.util;

import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.entity.AudioItem;


public interface AudioToAudioItem {
    AudioItem apply(Audio audio);
}
