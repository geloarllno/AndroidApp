package com.project.guardianalertcapstone;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView textConnectedDeviceName;
    private Button btnAddDevice;
    private Button btnDisconnect;
    private LinearLayout layoutConnectedDevice;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        textConnectedDeviceName = view.findViewById(R.id.text_connected_device_name);
        btnAddDevice = view.findViewById(R.id.btn_add_device);
        btnDisconnect = view.findViewById(R.id.btn_disconnect);
        LinearLayout layoutDeviceList = view.findViewById(R.id.layout_device_list);
        layoutConnectedDevice = view.findViewById(R.id.layout_connected_device);

        // Restore connection UI if already connected
        if (DeviceManager.getInstance().isConnected()) {
            layoutConnectedDevice.setVisibility(View.VISIBLE);
            textConnectedDeviceName.setText(getString(R.string.device_name));
        }

        btnAddDevice.setOnClickListener(v -> {
            AlertDialog.Builder selectionDialogBuilder = new AlertDialog.Builder(requireContext());
            View selectionDialogView = inflater.inflate(R.layout.dialog_device_selection, null);
            selectionDialogBuilder.setView(selectionDialogView);

            AlertDialog selectionDialog = selectionDialogBuilder.create();
            CardView cardGuardianEye = selectionDialogView.findViewById(R.id.card_guardian_eye);
            if (cardGuardianEye != null) {
                cardGuardianEye.setOnClickListener(selectView -> {
                    selectionDialog.dismiss();

                    AlertDialog.Builder connectionDialogBuilder = new AlertDialog.Builder(requireContext());
                    View connectionDialogView = inflater.inflate(R.layout.dialog_device_connection, null);
                    connectionDialogBuilder.setView(connectionDialogView);

                    AlertDialog connectionDialog = connectionDialogBuilder.create();

                    Button btnConnect = connectionDialogView.findViewById(R.id.btn_connect_wifi);
                    if (btnConnect != null) {
                        btnConnect.setOnClickListener(connectView -> {
                            progressBar.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "Connecting to Guardian Eye A100...", Toast.LENGTH_SHORT).show();

                            if (isNetworkAvailable()) {
                                String esp32Url = "http://172.20.10.6/status";
                                fetchEsp32Data(esp32Url, connectionDialog);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), getString(R.string.no_network_message), Toast.LENGTH_LONG).show();
                                connectionDialog.dismiss();
                            }
                        });
                    }

                    Button btnCancel = connectionDialogView.findViewById(R.id.btn_cancel_connection);
                    if (btnCancel != null) {
                        btnCancel.setOnClickListener(cancelView -> {
                            connectionDialog.dismiss();
                            Toast.makeText(getContext(), getString(R.string.cancelled_message), Toast.LENGTH_SHORT).show();
                        });
                    }

                    connectionDialog.setCancelable(false);
                    connectionDialog.show();
                });
            }

            selectionDialog.setCancelable(false);
            selectionDialog.show();
        });

        btnDisconnect.setOnClickListener(v -> {
            layoutConnectedDevice.setVisibility(View.GONE);
            DeviceManager.getInstance().resetConnection();
            Toast.makeText(getContext(), getString(R.string.disconnected_message), Toast.LENGTH_SHORT).show();

            NotificationHelper.showNotification(
                    requireContext(),
                    "Guardian Alert",
                    "Your Guardian Eye 100 has been disconnected! Please check your internet connection or battery!",
                    2
            );
        });

        return view;
    }

    private void fetchEsp32Data(String esp32Url, AlertDialog dialog) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                Log.d("HomeFragment", "Attempting to connect to: " + esp32Url);
                URL url = new URL(esp32Url);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);

                int responseCode = connection.getResponseCode();
                Log.d("HomeFragment", "Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder data = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line);
                    }

                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        layoutConnectedDevice.setVisibility(View.VISIBLE);
                        textConnectedDeviceName.setText(getString(R.string.device_name));
                        Toast.makeText(getContext(), "Connected successfully: " + data, Toast.LENGTH_LONG).show();
                        DeviceManager.getInstance().setConnected(true);
                        if (dialog != null) {
                            dialog.dismiss();
                        }

                        NotificationHelper.showNotification(
                                requireContext(),
                                "Guardian Alert",
                                "Guardian Eye 100 is successfully added to your app!",
                                1
                        );
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Connection failed: HTTP " + responseCode, Toast.LENGTH_LONG).show();
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error connecting to Guardian Eye A100: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.failed_to_connect, e.getMessage()), Toast.LENGTH_LONG).show();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d("HomeFragment", "No active network found");
                return false;
            }
            android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            boolean hasWifi = capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
            boolean hasCellular = capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR);
            Log.d("HomeFragment", "Network available - WiFi: " + hasWifi + ", Cellular: " + hasCellular);
            return capabilities != null && (hasWifi || hasCellular);
        }
        Log.d("HomeFragment", "ConnectivityManager is null");
        return false;
    }
}