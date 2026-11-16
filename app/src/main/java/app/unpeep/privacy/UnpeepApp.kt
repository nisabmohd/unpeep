package app.unpeep.privacy

import android.app.Application

class UnpeepApp : Application() {
    val container by lazy { AppContainer(this) }
}
