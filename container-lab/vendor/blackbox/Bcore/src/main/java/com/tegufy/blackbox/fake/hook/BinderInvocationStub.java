package com.tegufy.blackbox.fake.hook;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.util.Map;

import black.android.os.BRServiceManager;


public abstract class BinderInvocationStub extends ClassInvocationStub implements IBinder {
    private IBinder mBaseBinder;

    public BinderInvocationStub(IBinder baseBinder) {
        mBaseBinder = baseBinder;
    }

    @Override
    protected void onBindMethod() {
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        if (mBaseBinder == null) {
            return null;
        }
        return mBaseBinder.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        if (mBaseBinder == null) {
            return false;
        }
        return mBaseBinder.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        if (mBaseBinder == null) {
            return false;
        }
        return mBaseBinder.isBinderAlive();
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return (IInterface) getProxyInvocation();
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        if (mBaseBinder != null) {
            mBaseBinder.dump(fd, args);
        }
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        if (mBaseBinder != null) {
            mBaseBinder.dumpAsync(fd, args);
        }
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        if (mBaseBinder == null) {
            return false;
        }
        return mBaseBinder.transact(code, data, reply, flags);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        if (mBaseBinder != null) {
            mBaseBinder.linkToDeath(recipient, flags);
        }
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        if (mBaseBinder == null) {
            return false;
        }
        return mBaseBinder.unlinkToDeath(recipient, flags);
    }


    protected void replaceSystemService(String name) {
        Map<String, IBinder> services = BRServiceManager.get().sCache();
        services.put(name, this);
    }
}
