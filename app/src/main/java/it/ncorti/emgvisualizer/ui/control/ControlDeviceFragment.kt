package it.ncorti.emgvisualizer.ui.control

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import dagger.android.support.AndroidSupportInjection
import it.ncorti.emgvisualizer.BaseFragment
import it.ncorti.emgvisualizer.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_control_device.*
import javax.inject.Inject


class ControlDeviceFragment : BaseFragment<ControlDeviceContract.Presenter>(), ControlDeviceContract.View {

    companion object {
        fun newInstance() = ControlDeviceFragment()
    }

    @Inject
    lateinit var controlDevicePresenter: ControlDevicePresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        attachPresenter(controlDevicePresenter)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.layout_control_device, container, false)

        setHasOptionsMenu(true)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_connect.setOnClickListener { controlDevicePresenter.onConnectionToggleClicked() }
    }

    override fun showDeviceInformation(name: String?, address: String) {
        device_name.text = name ?: getString(R.string.unknown_device)
        device_address.text = address
    }

    override fun showConnectionProgress() {
        progress_connect.animate().alpha(1.0f)
    }

    override fun hideConnectionProgress() {
        progress_connect.animate().alpha(0.0f)
    }

    override fun showConnecting() {
        device_status.text = getString(R.string.connecting)
    }

    override fun showConnected() {
        device_status.text = getString(R.string.connected)
        button_connect.text = getString(R.string.disconnect)
    }

    override fun showDisconnected() {
        device_status.text = getString(R.string.disconnected)
        button_connect.text = getString(R.string.connect)
    }

    override fun showConnectionError() {
        Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show()
        button_connect.text = getString(R.string.connect)
    }

    override fun enableConnectButton() {
        button_connect.isEnabled = true
    }

    override fun disableConnectButton() {
        button_connect.isEnabled = false
    }
}