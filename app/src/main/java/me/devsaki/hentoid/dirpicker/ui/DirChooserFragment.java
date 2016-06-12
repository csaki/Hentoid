package me.devsaki.hentoid.dirpicker.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.dirpicker.events.CurrentRootDirChangedEvent;
import me.devsaki.hentoid.dirpicker.events.OnDirCancelEvent;
import me.devsaki.hentoid.dirpicker.events.OnDirChosenEvent;
import me.devsaki.hentoid.dirpicker.events.OpFailedEvent;
import me.devsaki.hentoid.dirpicker.events.UpdateDirTreeEvent;
import me.devsaki.hentoid.dirpicker.ops.DirListBuilder;
import me.devsaki.hentoid.dirpicker.util.Bus;
import me.devsaki.hentoid.util.LogHelper;

/**
 * Created by avluis on 06/12/2016.
 * Directory Chooser (Picker) Fragment Dialog
 */
public class DirChooserFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = LogHelper.makeLogTag(DirChooserFragment.class);
    private static final String CURRENT_ROOT_DIR = "currentRootDir";
    private static final String ROOT_DIR = "rootDir";

    private RecyclerView recyclerView;
    private TextView textView;
    private FloatingActionButton fab;
    private Button selectDirBtn;
    private EventBus bus;
    private File currentRootDir;

    public static DirChooserFragment newInstance(File rootDir) {
        DirChooserFragment dirChooserFragment = new DirChooserFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable(ROOT_DIR, rootDir);
        dirChooserFragment.setArguments(bundle);

        return dirChooserFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCurrentRootDir(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        Bus.register(bus, getActivity());
    }

    @Override
    public void onDestroy() {
        Bus.unregister(bus, getActivity());
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CURRENT_ROOT_DIR, currentRootDir);
        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.fragment_dir_picker, container, false);

        initUI(rootView);

        bus = new EventBus();
        bus.register(this);

        DirListBuilder dirListBuilder = new DirListBuilder(
                getActivity().getApplicationContext(), bus, recyclerView);
        Bus.register(bus, dirListBuilder);
        dirListBuilder.onUpdateDirTreeEvent(new UpdateDirTreeEvent(currentRootDir));

        return rootView;
    }

    private void initUI(View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.dir_list);
        textView = (TextView) rootView.findViewById(R.id.current_dir);
        fab = (FloatingActionButton) rootView.findViewById(R.id.create_dir);
        selectDirBtn = (Button) rootView.findViewById(R.id.select_dir);

        fab.setOnClickListener(this);
        selectDirBtn.setOnClickListener(this);
    }

    @Subscribe
    public void onOpFailedEvent(OpFailedEvent event) {
        LogHelper.d(TAG, getString(R.string.op_not_allowed));
        Toast.makeText(
                getActivity(), getString(R.string.op_not_allowed), Toast.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onCurrentRootDirChangedEvent(CurrentRootDirChangedEvent event) {
        currentRootDir = event.getCurrentDirectory();
        textView.setText(event.getCurrentDirectory().toString());
    }

    private void setCurrentRootDir(Bundle savedState) {
        if (savedState != null) {
            currentRootDir = (File) savedState.getSerializable(CURRENT_ROOT_DIR);
        } else {
            setCurrentDir();
        }
    }

    private void setCurrentDir() {
        File rootDir = (File) getArguments().getSerializable(ROOT_DIR);
        if (rootDir == null) {
            currentRootDir = Environment.getExternalStorageDirectory();
        } else {
            currentRootDir = rootDir;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        bus.post(new OnDirCancelEvent());
        super.onCancel(dialog);
    }

    @Override
    public void onClick(View v) {
        if (v == fab) {
            fabClicked();
        } else if (v == selectDirBtn) {
            selectDirBtnClicked();
        }
    }

    private void fabClicked() {
        new CreateDirDialog(getActivity(), bus,
                getActivity().getString(R.string.app_name)).dialog(currentRootDir);
    }

    private void selectDirBtnClicked() {
        bus.post(new OnDirChosenEvent(currentRootDir));
        dismiss();
    }
}