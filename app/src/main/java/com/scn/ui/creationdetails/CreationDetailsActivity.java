package com.scn.ui.creationdetails;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.scn.creationmanagement.Creation;
import com.scn.logger.Logger;
import com.scn.ui.BaseActivity;
import com.scn.ui.R;
import com.scn.ui.controller.ControllerActivity;
import com.scn.ui.controllerprofiledetails.ControllerProfileDetailsActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by steve on 2017. 12. 11..
 */

public class CreationDetailsActivity extends BaseActivity {

    //
    // Private members
    //

    private static final String TAG = CreationDetailsActivity.class.getSimpleName();

    private CreationDetailsViewModel viewModel;
    @Inject CreationDetailsAdapter creationDetailsAdapter;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.fab) FloatingActionButton floatingActionButton;
    @BindView(R.id.creation_name) TextView creationNameTextView;
    @BindView(R.id.edit) Button editCreationNameButton;
    @BindView(R.id.recyclerview) RecyclerView recyclerView;

    //
    // Activity overrides
    //

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Logger.i(TAG, "onCreate...");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_creation_details);
        ButterKnife.bind(this);

        setupActivityComponents();

        String creationName = getIntent().getStringExtra(EXTRA_CREATION_NAME);
        setupViewModel(creationName);
        setupRecyclerView();
    }

    @Override
    public void onBackPressed() {
        Logger.i(TAG, "onBackPressed...");
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_creation_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_play:
                if (!viewModel.checkIfCreationPlayable()) {
                    showAlertDialog(getString(R.string.no_controller_actions));
                    return true;
                }

                if (!viewModel.checkIfDevicesAreAvailable()) {
                    showAlertDialog(getString(R.string.missing_device));
                    return true;
                }

                Intent intent = new Intent(CreationDetailsActivity.this, ControllerActivity.class);
                intent.putExtra(EXTRA_CREATION_NAME, viewModel.getCreation().getName());
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //
    // Private methods
    //

    private void setupActivityComponents() {
        setSupportActionBar(toolbar);

        editCreationNameButton.setOnClickListener(view -> {
            showValueEnterDialog(
                    getString(R.string.enter_creation_name),
                    viewModel.getCreation().getName(),
                    newName -> {
                        if (newName.length() == 0) {
                            showAlertDialog(getString(R.string.creation_name_empty));
                            return;
                        }

                        if (newName.equals(viewModel.getCreation().getName())) {
                            return;
                        }

                        if (!viewModel.checkCreationName(newName)) {
                            showAlertDialog(getString(R.string.creation_name_exists));
                            return;
                        }

                        viewModel.renameCreation(newName);
                    });
        });

        floatingActionButton.setOnClickListener(view -> {
            showValueEnterDialog(
                    getString(R.string.enter_controller_profile_name),
                    "",
                    value -> {
                        if (value.length() == 0) {
                            showAlertDialog(getString(R.string.controller_profile_name_empty));
                            return;
                        }

                        if (!viewModel.checkControllerProfileName(value)) {
                            showAlertDialog(getString(R.string.controller_profile_name_exists));
                            return;
                        }

                        viewModel.addControllerProfile(value);
                    });
        });
    }

    private void setupViewModel(String creationName) {
        viewModel = getViewModel(CreationDetailsViewModel.class);
        viewModel.initialize(creationName);

        viewModel.getCreationMangerStateChangeLiveData().observe(CreationDetailsActivity.this, stateChange -> {
            Logger.i(TAG, "Creation manager stateChange - " + stateChange.getPreviousState() + " -> " + stateChange.getCurrentState());

            switch (stateChange.getCurrentState()) {
                case OK:
                    dismissDialog();

                    switch (stateChange.getPreviousState()) {
                        case INSERTING:
                            if (stateChange.isError()) {
                                showAlertDialog(
                                        getString(R.string.error_during_adding_controller_profile),
                                        dialogInterface -> stateChange.resetPreviousState());
                            }
                            else {
                                stateChange.resetPreviousState();
                                startControllerProfileDetailsActivity((long)stateChange.getData());
                            }
                            break;

                        case REMOVING:
                            if (stateChange.isError()) {
                                showAlertDialog(
                                        getString(R.string.error_during_removing_controller_profile),
                                        dialogInterface -> stateChange.resetPreviousState());
                            }
                            else {
                                stateChange.resetPreviousState();
                            }
                            break;

                        case UPDATING:
                            if (stateChange.isError()) {
                                showAlertDialog(
                                        getString(R.string.error_during_updating_creation),
                                        dialogInterface -> stateChange.resetPreviousState());
                            }
                            else {
                                //creationNameTextView.setText(viewModel.getCreation().getName());
                                stateChange.resetPreviousState();
                            }
                            break;
                    }
                    break;

                case INSERTING:
                    showProgressDialog(getString(R.string.saving));
                    break;

                case REMOVING:
                    showProgressDialog(getString(R.string.removing));
                    break;

                case UPDATING:
                    showProgressDialog(getString(R.string.updating));
                    break;
            }
        });

        viewModel.getCreationListLiveData().observe(CreationDetailsActivity.this, creations -> {
            Creation creation = viewModel.getCreation();
            creationNameTextView.setText(creation.getName());
            creationDetailsAdapter.setControllerProfile(creation.getControllerProfiles());
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(CreationDetailsActivity.this));
        recyclerView.addItemDecoration(new DividerItemDecoration(CreationDetailsActivity.this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(creationDetailsAdapter);

        creationDetailsAdapter.setListItemClickListener((controllerProfile, itemClickAction, data) -> {
            Logger.i(TAG, "onClick - " + controllerProfile);
            switch (itemClickAction) {
                case CLICK:
                    startControllerProfileDetailsActivity(controllerProfile.getId());
                    break;

                case REMOVE:
                    showQuestionDialog(
                            getString(R.string.are_you_sure_you_want_to_remove),
                            (dialogInterface, i) -> viewModel.removeControllerProfile(controllerProfile),
                            (dialogInterface, i) -> {});
                    break;
            }
        });
    }

    private void startControllerProfileDetailsActivity(long controllerProfileId) {
        Intent intent = new Intent(CreationDetailsActivity.this, ControllerProfileDetailsActivity.class);
        intent.putExtra(EXTRA_CONTROLLER_PROFILE_ID, controllerProfileId);
        startActivity(intent);
    }
}
