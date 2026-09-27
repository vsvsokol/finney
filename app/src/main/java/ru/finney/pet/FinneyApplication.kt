package ru.finney.pet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import ru.finney.pet.notifications.Notifier

class FinneyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannel(this)
        container.notifications.start()
        registerActivityLifecycleCallbacks(VisibilityTracker())
    }

    /** Видно ли приложение: хоть одна активность между onStart и onStop. */
    private inner class VisibilityTracker : ActivityLifecycleCallbacks {
        private var started = 0

        override fun onActivityStarted(activity: Activity) {
            started++
            container.appVisible = true
        }

        override fun onActivityStopped(activity: Activity) {
            started = (started - 1).coerceAtLeast(0)
            container.appVisible = started > 0
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
