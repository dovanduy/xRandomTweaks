package com.mpeter.xrandomtweaks.ui.main.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mpeter.xrandomtweaks.App;
import com.mpeter.xrandomtweaks.R;
import com.mpeter.xrandomtweaks.ui.HookPreferenceFragment;
import com.mpeter.xrandomtweaks.xposed.SupportedPackages;
import com.mpeter.xrandomtweaks.xposed.XposedModule;

import java.util.ArrayList;
import java.util.Collection;

import timber.log.Timber;

public class HomeFragment extends Fragment implements ModuleRecyclerViewAdapter.OnRecyclerViewItemClickListener {
    //Xposed state CardView
    TextView moduleState;
    TextView xposedVersionLabel;
    TextView xposedVersion;
    Button enableModule;

    //Tweaks CardView
    RecyclerView recyclerView;
    TextView tweakCount;
    ArrayList<String> apps = new ArrayList<>();

    SharedPreferences enabledTweaks;


    private OnFragmentInteractionListener mListener;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enabledTweaks = getContext().getSharedPreferences(App.ENABLED_PACKAGES_PREF_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        moduleState = view.findViewById(R.id.textview_module_state);
        xposedVersionLabel = view.findViewById(R.id.textview_xposed_version_label);
        xposedVersion = view.findViewById(R.id.textview_xposed_version);
        enableModule = view.findViewById(R.id.button_enable_module);
        tweakCount = view.findViewById(R.id.textview_tweak_count);

        recyclerView = view.findViewById(R.id.modules_recyclerview);

        boolean enabled = XposedModule.isModuleEnabled();

        if (enabled){
            moduleState.setText(R.string.module_state_enabled);
            moduleState.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_green500));
        } else {
            moduleState.setText(R.string.module_state_disabled);
            moduleState.setTextColor(ContextCompat.getColor(getActivity(), R.color.material_red500));
        }

        enableModule.setOnClickListener(v -> startActivity(
                new Intent().setComponent(
                        new ComponentName(
                                "de.robv.android.xposed.installer",
                                "de.robv.android.xposed.installer.WelcomeActivity")))
        );

        setupRecyclerView();
        setupTweakCount();

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClicked(String packageName) {
        Bundle bundle = new Bundle();
        bundle.putString(HookPreferenceFragment.EXTRA_PACKAGE_NAME, packageName);

        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, HookPreferenceFragment.instantiate(getActivity(), HookPreferenceFragment.class.getCanonicalName(), bundle))
                .commit();
    }

    public interface OnFragmentInteractionListener {
    }

    private void setupRecyclerView(){
        ArrayList<String> apps = SupportedPackages.getPackages();
        ArrayList<ApplicationInfo> appInfos = new ArrayList<>();

        PackageManager packageManager = getContext().getPackageManager();

        for (int i = 0; i < apps.size(); i++) {
            try {
                appInfos.add(packageManager.getPackageInfo(apps.get(i), PackageManager.GET_META_DATA).applicationInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Timber.e(e, "Package not found: %s", apps.get(i));
            }
        }

        ModuleRecyclerViewAdapter adapter = new ModuleRecyclerViewAdapter(appInfos, this, getContext());
        DividerItemDecoration decoration = new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setHasFixedSize(true);
    }

    private void setupTweakCount(){
        int allTweaksCount = SupportedPackages.getPackages().size();
        int enabledTweaksCount = 0;

        Collection<?> values = enabledTweaks.getAll().values();

        for (Object value : values) {
            if (value == Boolean.valueOf(true)) enabledTweaksCount++;
        }

        tweakCount.setText(getString(R.string.tweak_count, enabledTweaksCount, allTweaksCount));
    }
}
