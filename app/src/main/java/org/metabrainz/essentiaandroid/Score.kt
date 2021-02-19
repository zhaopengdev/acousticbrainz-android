package org.metabrainz.essentiaandroid

/**
 *@author zhaopeng
 *@date 2/17/21 12:20 PM
 *@description
 */
class Score {
    var duration : Double = 0.0
    var bpm : Double = 0.0;
    var minBeatTime = 0; //最小拍子的时间
    var pitches : ArrayList<PitchData>? = null
    var log : String = ""
}