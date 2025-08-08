package com.odnovolov.forgetmenot.presentation.screen.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.odnovolov.forgetmenot.BuildConfig
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.openEmailComposer
import com.odnovolov.forgetmenot.presentation.common.openUrl
import com.odnovolov.forgetmenot.presentation.common.setStatusBarColor
import com.odnovolov.forgetmenot.presentation.common.setTransparentStatusBar
import com.odnovolov.forgetmenot.presentation.screen.navhost.NavHostFragment
import com.odnovolov.forgetmenot.presentation.screen.navhost.NavHostFragment.NavigationDestination
import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setStatusBarColor(requireActivity(), R.color.app_bar_about_screen)
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        appVersionTextView.text = "v" + BuildConfig.VERSION_NAME
        developerButton.setOnClickListener {
            openEmailComposer(receiver = DEVELOPER_EMAIL)
        }
        sourceCodeButton.setOnClickListener {
            openUrl(SOURCE_CODE_URL)
        }
        privacyPolicyButton.setOnClickListener {
            openUrl(PRIVACY_POLICY_URL)
        }
        supportAppButton.setOnClickListener {
            (parentFragment as NavHostFragment).navigateTo(NavigationDestination.SupportApp)
        }
        thanksToTranslatorsTextView.movementMethod = LinkMovementMethod.getInstance()
        translationRecycler.adapter = TranslationAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRemoving) {
            setTransparentStatusBar(requireActivity())
        }
    }

    companion object {
        const val DEVELOPER_EMAIL = "odnovolov.artem@gmail.com"
        const val SOURCE_CODE_URL = "https://github.com/tema6120/ForgetMeNot"
        const val PRIVACY_POLICY_URL =
            "https://github.com/tema6120/ForgetMeNot/blob/master/PRIVACY_POLICY.md"
    }
}