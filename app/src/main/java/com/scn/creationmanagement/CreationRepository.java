package com.scn.creationmanagement;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.scn.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by imurvai on 2017-12-17.
 */

@Singleton
final class CreationRepository {

    //
    // Members
    //

    private static final String TAG = CreationRepository.class.getSimpleName();

    private final CreationDao creationDao;
    private final List<Creation> creationList = new ArrayList<>();
    private final MutableLiveData<List<Creation>> creationListLiveData = new MutableLiveData<>();

    //
    // Constructor
    //

    @Inject
    CreationRepository(@NonNull Context context) {
        Logger.i(TAG, "constructor...");

        CreationDatabase database = Room.databaseBuilder(context, CreationDatabase.class, CreationDatabase.DatabaseName).build();
        creationDao = database.creationDao();

        creationListLiveData.setValue(new ArrayList<>());
    }

    //
    // API
    //

    @WorkerThread
    public synchronized void loadCreations() {
        Logger.i(TAG, "loadCreations...");

        creationList.clear();

        List<Creation> creations = creationDao.getCreations();
        for (Creation creation : creations) {
            List<ControllerProfile> controllerProfiles = creationDao.getControllerProfiles(creation.getId());
            for (ControllerProfile controllerProfile : controllerProfiles) {

                List<ControllerEvent> controllerEvents = creationDao.getControllerEvents(controllerProfile.getId());
                for (ControllerEvent controllerEvent : controllerEvents) {

                    List<ControllerAction> controllerActions = creationDao.getControllerActions(controllerEvent.getId());
                    for (ControllerAction controllerAction : controllerActions) {
                        controllerEvent.addControllerAction(controllerAction);
                    }

                    controllerProfile.addControllerEvent(controllerEvent);
                }

                creation.addControllerProfile(controllerProfile);
            }

            creationList.add(creation);
        }

        creationListLiveData.postValue(creationList);
    }

    @WorkerThread
    public synchronized void insertCreation(@NonNull Creation creation) {
        Logger.i(TAG, "saveCreation - " + creation);
        creation.setId(creationDao.insertCreation(creation));
        creationList.add(creation);
        creationListLiveData.postValue(creationList);
    }

    @WorkerThread
    public synchronized void updateCreation(@NonNull Creation creation, @NonNull String newName) {
        Logger.i(TAG, "updateCreation - " + creation + ", new name: " + newName);

        String originalName = creation.getName();
        try {
            creation.setName(newName);
            creationDao.updateCreation(creation);
        }
        catch (Exception e) {
            Logger.e(TAG, "  Could not update creation - " + creation);
            creation.setName(originalName);
            throw e;
        }

        creationListLiveData.postValue(creationList);
    }

    @WorkerThread
    public synchronized void removeCreation(@NonNull Creation creation) {
        Logger.i(TAG, "removeCreation - " + creation);
        creationDao.deleteCreationRecursive(creation.getId());
        creationList.remove(creation);
        creationListLiveData.postValue(creationList);
    }

    @WorkerThread
    public synchronized void insertControllerProfile(@NonNull Creation creation, @NonNull ControllerProfile controllerProfile) {
        Logger.i(TAG, "insertControllerProfile - " + controllerProfile);
        controllerProfile.setId(creationDao.insertControllerProfile(controllerProfile));
        creation.addControllerProfile(controllerProfile);
    }

    @WorkerThread
    public synchronized void updateControllerProfile(@NonNull Creation creation, @NonNull ControllerProfile controllerProfile, @NonNull String newName) {
        Logger.i(TAG, "updateControllerProfile - " + controllerProfile + ", new name: " + newName);

        String originalName = controllerProfile.getName();
        try {
            controllerProfile.setName(newName);
            creationDao.updateControllerProfile(controllerProfile);
        }
        catch (Exception e) {
            Logger.e(TAG, "  Could not update controller profile - " + controllerProfile);
            controllerProfile.setName(originalName);
        }
    }

    @WorkerThread
    public synchronized void removeControllerProfile(@NonNull Creation creation, @NonNull ControllerProfile controllerProfile) {
        Logger.i(TAG, "removeControllerProfile - " + controllerProfile);
        creationDao.deleteControllerProfileRecursive(controllerProfile.getId());
        creation.removeControllerProfile(controllerProfile);
    }

    @WorkerThread
    public synchronized void insertControllerEvent(@NonNull ControllerProfile controllerProfile, @NonNull ControllerEvent controllerEvent) {
        Logger.i(TAG, "insertControllerEvent - " + controllerEvent);
        controllerEvent.setId(creationDao.insertControllerEvent(controllerEvent));
        controllerProfile.addControllerEvent(controllerEvent);
    }

    public synchronized Creation getCreation(@NonNull String creationName) {
        Logger.i(TAG, "getCreation - " + creationName);

        for (Creation creation : creationList) {
            if (creation.getName() == creationName) return creation;
        }

        Logger.w(TAG, "  Could not find creation.");
        return null;
    }

    public synchronized LiveData<List<Creation>> getCreationListLiveData() {
        Logger.i(TAG, "getCreationListLiveData...");
        return creationListLiveData;
    }
}