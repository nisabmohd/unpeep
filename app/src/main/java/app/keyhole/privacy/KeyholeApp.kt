package app.keyhole.privacy

import android.app.Application

class KeyholeApp : Application() {
    val container by lazy { AppContainer(this) }
}
