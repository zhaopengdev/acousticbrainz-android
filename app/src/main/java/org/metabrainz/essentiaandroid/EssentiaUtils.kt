package org.metabrainz.essentiaandroid

import android.util.ArrayMap
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.lang.StringBuilder
import kotlin.math.abs

object EssentiaUtils {
    const val TAG = "EssentiaUtils"
    const val DEFAULT_BMP = 120;
    const val DEFAULT_BEAT_ONE = 60 / DEFAULT_BMP;

    enum class Note{
//        WHOLE,HALF,
        QUARTER,EIGHTH,SIXTEENTH
    }

    enum class ScoreType{
        TYPE_44,TYPE_34
    }

    
    fun parseEssentiaResult(json :String ) : EssentiaRawResult{

        var jo =  JSONObject(json)
        val jp : JSONArray = jo.getJSONArray("pitches")
        var duration = jo.getDouble("duration")
        var ds = DoubleArray(jp.length())
        var bpm = jo.getDouble("bpm")

        for(i in 0 until  jp.length()){
            ds[i] = jp.getDouble(i)
            Log.d(TAG,"value:${jp[i]} ")
        }
        val er = EssentiaRawResult(duration,ds,bpm)

        return er;
    }

    fun computeScore(json : String) : Score{
        val er = parseEssentiaResult(json)
        return computeScore(er,er.bmp,ScoreType.TYPE_44,Note.EIGHTH)
    }

    fun computeScore(result: EssentiaRawResult,b: Double,type : ScoreType,minNote : Note) : Score{
        val score = Score();
        score.duration = result.duration
        score.bpm = b
        val unitInterval = computeMinInterval(score.bpm,type,minNote)
        val durationMs = score.duration * 1000
        val times = linspace(0.0,score.duration,result.pitches.size)
        val beatCount : Int = if(score.duration % unitInterval == 0.0){
            (score.duration / unitInterval).toInt()
        }else{
            (score.duration / unitInterval + 1).toInt()
        }

        val scoreTimes = linspace(0.0,score.duration,beatCount)

        var indexOfScoreTimes = 0
        var startIndexInPitches = 0
        val scorePitches = DoubleArray(beatCount)

        Log.d(TAG,"computeScore unitInterval:$unitInterval,times size:${times.size}" +
                ",beatCount:$beatCount,scoreTimes size:${scoreTimes.size},bpm :$b")

        for(i in times.indices){
            if(times[i] >= scoreTimes[indexOfScoreTimes]){
                scorePitches[indexOfScoreTimes] = computePianoPitch(result.pitches,startIndexInPitches, i)
                Log.d(TAG, "computeScore scorePitch startIndexInPitches:$startIndexInPitches,endIndex:$i ,indexOfScoreTimes:$indexOfScoreTimes,value:${scorePitches[indexOfScoreTimes]}")
                startIndexInPitches = i + 1
                indexOfScoreTimes++
            }
        }

        val sb = StringBuilder()
        val scoreToPlay = ArrayList<PitchData>()
        for(i in scorePitches.indices){
            val frequency = scorePitches[i]
            val p = findPitchFromFrequency(frequency)
            p.time = scoreTimes[i]
            scoreToPlay.add(p)
            sb.append("time:").append(p.time).append(",pitch:").append(p.pitch.name).append('\n')
        }
        score.log = sb.toString()
        Log.e(TAG, "computeScore scoreToPlay result size: ${scoreToPlay.size},result:${sb}")
        score.pitches = scoreToPlay
        return score
    }

    fun linspace( begin : Double, finish : Double, number : Int) : DoubleArray{
        val interval : Double= (finish - begin) / (number - 1)
        val array = DoubleArray(number)
        for (i in 0 until number) {
            array[i] = begin+i*interval
        }
        return array
    }

    /**TODO
     * 给定一小段时间的频率，给出当前时间段的音高
     */
    fun computePianoPitch(pitches: DoubleArray,start : Int, end : Int) : Double{
        val map = ArrayMap<Double,Int>()
        //找到出现次数最多的pitch
        for(i in start .. end){
            val p = pitches[i];
            if(map.contains(p)){
                val last : Int = map[p]!!
                map[p] = last + 1
            }else{
                map[p] = 1
            }
        }
        var max = 0;
        var key  = 0.0

        for(i in 0 until map.size){
            val it = map.entries.elementAt(i)
            if(it.value > max){
                key = it.key
                max = it.value
            }
        }
        Log.d(TAG,"computePianoPitch times:$max,pitch:$key")
        return key
    }

    /**TODO
     * 根据频率，获取对应音高值
     */
    fun findPitchFromFrequency(freq : Double) : PitchData{
        val pitchesPiano = Piano.Pitches;
        if(freq > 25) {
            for (i in pitchesPiano.indices) {
                val p = pitchesPiano[i]
                if (p.frequency > freq) {
                    return if(i == 0){
                        PitchData(pitchesPiano[0])
                    }else if(i == pitchesPiano.size -1){
                        PitchData(pitchesPiano[pitchesPiano.size - 1])
                    }else{
                        val next = i + 1;
                        val nextFrequency = pitchesPiano[next].frequency
                        val diffNext = abs(nextFrequency - freq)
                        val diff = abs(p.frequency - freq)
                        if(diff < diffNext){
                            PitchData(p)
                        }else{
                            PitchData(pitchesPiano[next])
                        }
                    }
                }
            }
        }
        return PitchData(Pitch(-1,"mute",-1.0))
    }

    /**
     * 计算出最小播放时间时长ms
     * @param bmp
     * @param barNumber 小节数 ，比如4/4拍 就是4，6/8拍是8
     * @param noteNumber 最小音符数、比如八分音符是8、16分音符是16
     */
    fun computeMinInterval(bmp : Double,type : ScoreType,minNote : Note) : Double{
        val oneBeatTime = 60.0  / bmp // 一拍的时间

        val scaleByType : Float = when(type){
            ScoreType.TYPE_34-> 4 / 3f
            ScoreType.TYPE_44 -> 1f
        }

        val scaleByNote : Float = when(minNote){
            Note.QUARTER -> {
                1f
            }
            Note.EIGHTH -> {
                0.5f
            }
            Note.SIXTEENTH -> {
                0.25f
            }
        }
        return (oneBeatTime  * scaleByNote * scaleByType).toDouble()
    }







}