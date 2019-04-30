package com.instructure.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.instructure.canvasapi2.models.AccountDomain
import com.instructure.loginapi.login.dialog.ErrorReportDialog
import com.instructure.loginapi.login.util.Const
import com.instructure.pandautils.utils.onClick
import com.instructure.student.activity.SignInActivity
import kotlinx.android.synthetic.main.activity_made_easy_login_init.*

class MadeEasyLoginInit : AppCompatActivity(), ErrorReportDialog.ErrorReportDialogResultListener {

    override fun onTicketError() {
        TODO("not implemented")
    }

    override fun onTicketPost() {
        TODO("not implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_made_easy_login_init)
        bindViews()
    }

    private fun bindViews() {
        bt_login.onClick {
            val intent = SignInActivity.createIntent(this, AccountDomain(Const.URL_CANVAS_NETWORK))
            startActivity(intent)
        }

        bt_phone.onClick {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+919495993606")
            startActivity(intent)
        }

        bt_map.onClick {
            val intent = Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=Made+Easy&query_place_id=ChIJ5afYLDe7BTsRhIxz6D4Welg"))
            startActivity(intent)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MadeEasyLoginInit::class.java)
        }
    }
}
