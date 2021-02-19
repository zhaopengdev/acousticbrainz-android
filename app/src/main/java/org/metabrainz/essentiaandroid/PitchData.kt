package org.metabrainz.essentiaandroid
 class PitchData(
                val pitch: Pitch
                ){
     var time : Double = 0.0

     override fun toString(): String {
         return "PitchData(pitch=$pitch, time=$time)"
     }


 }
