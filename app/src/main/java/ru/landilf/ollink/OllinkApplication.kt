package ru.landilf.ollink

import android.app.Application
import ru.landilf.ollink.di.AppContainer

class OllinkApplication : Application() {
    val container by lazy { AppContainer(this) }
}
