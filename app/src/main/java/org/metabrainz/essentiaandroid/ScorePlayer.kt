package org.metabrainz.essentiaandroid

import android.content.Context
import android.media.SoundPool
import android.util.ArrayMap
import android.util.Log
import java.util.*

/**
 *@author zhaopeng
 *@date 2/18/21 10:42 PM
 *@description
 */
class ScorePlayer(private val context :Context) {

    companion object{
        const val TAG = "ScorePlayer"
    }

    var timer : Timer? = null
    var mSoundPool : SoundPool? = null
    val soundMap = ArrayMap<String,Int>()
    var isSoundLoaded = false;

    fun play(score: Score){



        timer?.cancel()
        timer = Timer()
        if(mSoundPool == null) {
            mSoundPool = SoundPool.Builder().setMaxStreams(20).build()
        }

        val midiCount = Piano.Pitches.size
        Log.d(TAG,"start loadSound")
        if(!isSoundLoaded) {
            for (i in Piano.Pitches.indices) {
                val p = Piano.Pitches[i]

                val path = "notes/${p.name}.MP3"
                val key = p.name

                val streamId = mSoundPool!!.load(context.applicationContext.assets.openFd(path), 1)
                soundMap[key] = streamId
            }
        }
        var m = 0
        if(!isSoundLoaded) {
            mSoundPool!!.setOnLoadCompleteListener { soundPool, sampleId, status ->
                Log.d(TAG, "loadSound done m:$m,midiCount:$midiCount")
                if(m == midiCount -1) {
                    isSoundLoaded = true
                    timer?.schedule(Task(score, mSoundPool!!, soundMap), 0, 10)
                }
                m++
            }
        }else{
            timer?.schedule(Task(score, mSoundPool!!, soundMap), 0, 10)
        }


    }


    class Task(private val score: Score,private val soundPool: SoundPool,private val soundMap: ArrayMap<String,Int>) : TimerTask() {
//        var pastTimeTotal : Long = 0
        var pastPitch : Int = 0
        var startTime : Long = 0
        var lastTime : Long = 0
        override fun run() {
            if(pastPitch > score.pitches!!.size -1){
                cancel()
                Log.e(TAG, "playTask finished")
                return
            }
            val now = System.currentTimeMillis()
            val pitch = score.pitches!![pastPitch]
            val next = pitch.time * 1000
            if(startTime == 0L){
                startTime = now
            }
            lastTime = now
//            val delta = now - lastTime
            val nextPitchTime = startTime + next
            if(now > nextPitchTime){
                //过了目标时间点了，播放
                Log.e(TAG, "play pitch:$pitch")
                if(pitch.pitch.midi > 0){
                    val sid = soundMap[pitch.pitch.name]
                    soundPool.play(sid!!,10f,10f,1,0,1.0f)
                }
                pastPitch++
            }

        }

    }
}