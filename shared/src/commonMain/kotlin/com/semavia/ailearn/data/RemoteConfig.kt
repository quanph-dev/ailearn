package com.semavia.ailearn.data

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf

/**
 * Remote configuration parameters mirror the original app defaults and keys.
 */
object RemoteConfig {
    val revision: MutableIntState = mutableIntStateOf(0)

    var surveyBeforeHome: Boolean = true
    var freeSlotAiTutor: Int = 10
    var hintLesson: Boolean = true
    var hintDiamondCost: Int = 5
    var rewardAd: Boolean = true
    var rewardAd2Id: Boolean = false
    var loginDemo: Boolean = false
    var nameAccountDemo: String = ""
    var passwordDemo: String = ""
    var rateExit: Boolean = true
    var bannerAll: Boolean = true
    var bannerAll2Id: Boolean = false
    var interFeature: Boolean = true
    var interFeature2Id: Boolean = false
    var jumInterFeature: Int = 3
    var interClickLesson: Boolean = true
    var offlineRobot: Boolean = false
    var homeLayout: String = "v2"
    var nativeHome: Boolean = true
    var nativeHome2Id: Boolean = false
    var nativeSurveyCol: Boolean = false
    var nativeSurveyCol2Id: Boolean = false
    var typeAdInSurvey: String = ""
    var nativeResume2Id: Boolean = false
    var foBannerSplash: Boolean = true
    var foInterSplash: Boolean = true
    var foHfInterSplash: Boolean = true
    var foNativeLanguage1: Boolean = true
    var foHfNativeLanguage1: Boolean = true
    var foNativeLanguage2: Boolean = true
    var foHfNativeLanguage2: Boolean = true
    var foEnableLfo: Boolean = true
    var foEnableOnboarding: Boolean = true
    var foNativeOnboarding1: Boolean = true
    var foHfNativeOnboarding1: Boolean = true
    var foNativeFullScr1: Boolean = false
    var foNativeFullScr2: Boolean = false
    var foHfNativeFullScr1: Boolean = false
    var foHfNativeFullScr2: Boolean = false
    var foRetryAdSurvey: Boolean = true
    var foGroupExpression: Boolean = false
    var foTutorial: Boolean = false
}
