#include <essentia/essentia.h>
#include <essentia/algorithm.h>
#include <essentia/algorithmfactory.h>
#include <essentia/utils/extractor_music/extractor_version.h>
#include "extractor_utils.h"
#include "credit_libav.h"
#include <android/log.h>

using namespace std;
using namespace essentia;
using namespace essentia::standard;

void usage(char *progname) {
    cout << "Error: wrong number of arguments" << endl;
    cout << "Usage: " << progname << " input_audiofile output_textfile [profile]" << endl;
    cout << endl << "Music extractor version '" << MUSIC_EXTRACTOR_VERSION << "'" << endl
         << "built with Essentia version " << essentia::version_git_sha << endl;

    creditLibAV();
    exit(1);
}

//linspace函数
void linspace(Real begin, Real finish, int number,std::vector<Real>& array) {
        Real interval = (finish - begin) / (number - 1);//
//    Mat f(1, number, CV_64FC1);
//    Real f[number];
        for (int j = 0; j < number; j++) {
//            f.at<double>(i,j)=begin+j*interval;
            array.at(j) = begin+j*interval;
            //cout << "("<<i+1<<","<<j+1<<")"<<f.at<double>(i, j) << endl;
        }
}

int essentia_pitch(string audioFilename, string outputFilename, string profileName,int hopSize) {
    try{
        Real sample = 44100;
        essentia::init();
        setDebugLevel(EAlgorithm);
        Pool options;
//        setExtractorDefaultOptions(options);
        options.set("indent", 4);
        options.set("outputFormat", "json");
        setExtractorOptions("", options);

        AlgorithmFactory& factory = standard::AlgorithmFactory::instance();

        Algorithm* pitch = factory.create("PredominantPitchMelodia",
                "hopSize",hopSize
                );
        pitch->configure("referenceFrequency",27.5);
        pitch->configure("frameSize",8192);


        Pool results;
        Algorithm* audio = factory.create("MonoLoader",
                                          "filename", audioFilename,
                                          "sampleRate", sample);

        Algorithm* aBpm = factory.create("RhythmExtractor2013");

        std::vector<Real> audioBuffer;
        Real bpm,confidence;

        std::vector<Real> pitchResult,pitchConfidenceResult;
        std::vector<Real> ticks,estimates,bpmIntervals;



        audio->output("audio").set(audioBuffer);
//        fc->input("signal").set(audioBuffer);
        pitch->input("signal").set(audioBuffer);
        aBpm->input("signal").set(audioBuffer);

        aBpm->output("bpm").set(bpm);
        aBpm->output("ticks").set(ticks);
        aBpm->output("estimates").set(estimates);
        aBpm->output("bpmIntervals").set(bpmIntervals);
        aBpm->output("confidence").set(confidence);

        pitch->output("pitch").set(pitchResult);
        pitch->output("pitchConfidence").set(pitchConfidenceResult);
//        pitch->output("resultsFrames").set(resultsFrames);

        audio->compute();
        aBpm->compute();
        pitch->compute();
//
////        __android_log_write(ANDROID_LOG_ERROR, "pitch","pitchConfidence size:"+pitchConfidenceResult.size() );
//

        Real audioDuration = audioBuffer.size() / sample;
//        linspace(0,audioDuration,pitchResult.size(),pitchTimes);

        results.set("pitches",pitchResult);
        results.set("bpm",bpm);
//        results.set("pitchConfidence",pitchConfidenceResult);
//        results.set("pitchTimes",pitchTimes);
//        results.set("pitchLength",pitchResult.size());
        results.set("duration",audioDuration);

//        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", "pitchResult length:"+pitchResult.size());
//        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", "pitchConfidence length:"+pitchConfidenceResult.size());
//
        outputToFile(results, outputFilename, options);
//
        delete pitch;
        delete audio;
        essentia::shutdown();
    }

    catch (EssentiaException& e) {
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", e.what());
        return 1;
    }
    catch (const std::bad_alloc& e) {
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", "bad_alloc exception: Out of memory");
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", e.what());
        return 1;
    }

    return 0;

}


int essentia_main(string audioFilename, string outputFilename, string profileName) {
    // Returns: 1 on essentia error

    try {
        essentia::init();
        setDebugLevel(EExecution);

        cout.precision(10); // TODO ????

        Pool options;
        setExtractorDefaultOptions(options);
        setExtractorOptions("", options);

        Algorithm* extractor = AlgorithmFactory::create("MusicExtractor",
                                                        "profile", "");

        Pool results;
        Pool resultsFrames;

        extractor->input("filename").set(audioFilename);
        extractor->output("results").set(results);
        extractor->output("resultsFrames").set(resultsFrames);

        extractor->compute();

        mergeValues(results, options);

        outputToFile(results, outputFilename, options);
        if (options.value<Real>("outputFrames")) {
            outputToFile(resultsFrames, outputFilename+"_frames", options);
        }
        delete extractor;
        essentia::shutdown();
    }
    catch (EssentiaException& e) {
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", e.what());
        return 1;
    }
    catch (const std::bad_alloc& e) {
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", "bad_alloc exception: Out of memory");
        __android_log_write(ANDROID_LOG_ERROR, "Essentia Android", e.what());
        return 1;
    }

    return 0;
}
