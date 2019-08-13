package com.markx0823.myopenvpn;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import de.blinkt.openvpn.VpnActivity;
import de.blinkt.openvpn.api.APIVpnProfile;
import de.blinkt.openvpn.api.IOpenVPNAPIService;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "#####";

    Context ctx = this;
    private IOpenVPNAPIService iOpenVPNAPIService;

    private static final int OPENVPN_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonVpnActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ctx, VpnActivity.class));
            }
        });

        findViewById(R.id.buttonConnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listVPNs();
            }
        });

        findViewById(R.id.buttonDisconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iOpenVPNAPIService == null) {
                    return;
                }

                try {
                    iOpenVPNAPIService.disconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    protected void onStop() {
        unbindService();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case OPENVPN_PERMISSION:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(ctx, "Connect to OpenVPN", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void listVPNs() {
        if (iOpenVPNAPIService == null) {
            return;
        }

        try {
            List<APIVpnProfile> list = iOpenVPNAPIService.getProfiles();

            // Debug message
            for (APIVpnProfile profile : list) {
                Log.d(LOG_TAG, profile.mUUID + ", " + profile.mName);
            }

            if (list.isEmpty()) {
                return;
            }

            iOpenVPNAPIService.startProfile(list.get(0).mUUID);
        } catch (RemoteException e) {
            e.printStackTrace();
            unbindService();
            bindService();
        }
    }

    private void bindService() {
        Intent intent = new Intent(IOpenVPNAPIService.class.getName());
        intent.setPackage("de.blinkt.openvpn");

        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        if (iOpenVPNAPIService == null) {
            return;
        }

        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iOpenVPNAPIService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                Intent intent = iOpenVPNAPIService.prepare(ctx.getPackageName());
                if (intent != null) {
                    startActivityForResult(intent, OPENVPN_PERMISSION);
                } else {
                    onActivityResult(OPENVPN_PERMISSION, RESULT_OK, null);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                iOpenVPNAPIService = null;
                bindService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iOpenVPNAPIService = null;
        }
    };
}
