package org.sagebionetworks.research.sageresearch.profile

import androidx.lifecycle.Observer
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_profilesettings_list.*
import kotlinx.android.synthetic.main.fragment_profilesettings_list.view.*
import org.sagebionetworks.bridge.rest.model.SurveyReference
import org.sagebionetworks.research.mobile_ui.show_step.view.SystemWindowHelper
import org.sagebionetworks.research.sageresearch.profile.ProfileSettingsRecyclerViewAdapter.Companion.VIEW_TYPE_SECTION
import org.sagebionetworks.research.sageresearch_app_sdk.R


abstract class ProfileSettingsFragment : OnListInteractionListener, EditProfileItemDialogListener, androidx.fragment.app.Fragment()  {

    private var profileKey = "ProfileDataSource" //Initialized to the default key
    private var isMainView = true;
    var adapter: ProfileSettingsRecyclerViewAdapter? = null

    protected lateinit var profileViewModel: ProfileViewModel

    override abstract fun launchSurvey(surveyReference: SurveyReference)
    abstract fun newInstance(profileKey: String, isMainView: Boolean): ProfileSettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            profileKey = it.getString(ARG_PROFILE_KEY)
            isMainView = it.getBoolean(ARG_IS_MAIN_VIEW, true)
        }


    }

    abstract fun loadProfileViewModel(): ProfileViewModel

    fun showLoading(show: Boolean) {
        Handler(Looper.getMainLooper()).post {spinner?.visibility = if (show) View.VISIBLE else View.GONE }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        profileViewModel = loadProfileViewModel()

        val view = inflater.inflate(R.layout.fragment_profilesettings_list, container, false)

        if (!isMainView) {
            view.back_icon.visibility = View.VISIBLE
            view.textView.visibility = View.INVISIBLE
            view.settings_icon.visibility = View.GONE
            view.back_icon.setOnClickListener {
                activity?.onBackPressed()
            }
        }

        val topListener = SystemWindowHelper.getOnApplyWindowInsetsListener(SystemWindowHelper.Direction.TOP)
        ViewCompat.setOnApplyWindowInsetsListener(view.textView, topListener)


        return view
    }

    override fun launchEditProfileItemDialog(value: String, profileItemKey: String) {
        val dialogFragment = EditProfileItemDialogFragment.newInstance(value, profileItemKey, this)
        dialogFragment.show(requireFragmentManager(), "EditDialog")
    }

    override  fun saveEditDialogValue(value: String, profileItemKey: String) {
        profileViewModel.saveStudyParticipantValue(value, profileItemKey)
        adapter?.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings_icon.setOnClickListener {
            val settingsFragment = newInstance("SettingsDataSource", false)
            addChildFragmentOnTop(settingsFragment, "settingsFragment")
        }

        showLoading(true)
        // Set the adapter
        if (view.list is androidx.recyclerview.widget.RecyclerView) {
            with(view.list) {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

                val divider = object : androidx.recyclerview.widget.DividerItemDecoration(this.getContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL) {

                    override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
                        val pos = getChildAdapterPosition(view)
                        if (parent.adapter?.getItemViewType(pos) == VIEW_TYPE_SECTION) {
                            if (pos == 0) {
                                outRect.set(0, 0, 0, 0)
                            } else {
                                outRect.set(0, 50, 0, 0)
                            }
                        } else {
                            super.getItemOffsets(outRect, view, parent, state)
                        }
                    }

                }
                val drawable = requireContext().resources.getDrawable(R.drawable.form_step_divider)
                divider.setDrawable(drawable)
                this.addItemDecoration(divider)

            }
            if (adapter == null) {
                profileViewModel.profileData(profileKey).observe(this, Observer { loader ->
                    if (adapter == null) {
                        adapter = ProfileSettingsRecyclerViewAdapter(loader, this)
                        view.list.adapter = adapter
                    } else {
                        adapter?.updateDataLoader(loader)
                    }
                    showLoading(false)
                })
            } else {
                view.list.adapter = adapter
                showLoading(false)
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    /**
     * Adds a child fragment on top of this fragment and adds this fragment to the back stack with the provided tag.
     * @param childFragment The fragment to add on top of this fragment.
     * @param tag The tag for this fragment on the back stack.
     */
    fun addChildFragmentOnTop(childFragment: androidx.fragment.app.Fragment, tag: String?) {
        requireFragmentManager()
                .beginTransaction()
                .detach(this)
                .add((this.requireView().parent as ViewGroup).id, childFragment)
                .addToBackStack(null)
                .commit()
    }

    companion object {

        const val ARG_PROFILE_KEY = "profile_key"
        const val ARG_IS_MAIN_VIEW = "is_main_view"

    }
}
