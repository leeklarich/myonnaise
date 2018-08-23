package it.ncorti.emgvisualizer.ui.control

import it.ncorti.emgvisualizer.BasePresenter
import it.ncorti.emgvisualizer.BaseView


interface ControlDeviceContract {

    interface View : BaseView {

        fun showDeviceInformation(name: String?, address: String)

        fun showConnectionProgress()

        fun hideConnectionProgress()

        fun showConnected()

        fun showDisconnected()

        fun showConnecting()

        fun showConnectionError()

        fun enableConnectButton()

        fun disableConnectButton()
    }

    abstract class Presenter(override val view: BaseView) : BasePresenter<BaseView>(view) {

        abstract fun onConnectionToggleClicked()

        abstract fun onStreamingToggleClicked()

    }
}