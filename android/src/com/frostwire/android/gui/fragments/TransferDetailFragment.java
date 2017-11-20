/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.gui.fragments;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.adapters.menu.CancelMenuAction;
import com.frostwire.android.gui.adapters.menu.CopyToClipboardMenuAction;
import com.frostwire.android.gui.adapters.menu.OpenMenuAction;
import com.frostwire.android.gui.adapters.menu.PauseDownloadMenuAction;
import com.frostwire.android.gui.adapters.menu.ResumeDownloadMenuAction;
import com.frostwire.android.gui.adapters.menu.SeedAction;
import com.frostwire.android.gui.adapters.menu.SendBitcoinTipAction;
import com.frostwire.android.gui.adapters.menu.SendFiatTipAction;
import com.frostwire.android.gui.transfers.UIBittorrentDownload;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractFragment;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.bittorrent.PaymentOptions;
import com.frostwire.transfers.TransferState;

/**
 * @author gubatron
 * @author aldenml
 * @author marcelinkaaa
 */
public final class TransferDetailFragment extends AbstractFragment {

    private UIBittorrentDownload uiBittorrentDownload;

    public TransferDetailFragment() {
        super(R.layout.fragment_transfer_detail);
        setHasOptionsMenu(true);
    }

    public void setUiBittorrentDownload(UIBittorrentDownload uiBittorrentDownload) {
        this.uiBittorrentDownload = uiBittorrentDownload;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_transfer_detail_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private boolean isPausable() {
        return !isPaused() || uiBittorrentDownload.getState() == TransferState.SEEDING;
    }

    private boolean isResumable() {
        if (!uiBittorrentDownload.isComplete() && !uiBittorrentDownload.isSeeding() && isPaused()) {
            return false;
        }
        NetworkManager networkManager = NetworkManager.instance();
        boolean wifiIsUp = networkManager.isDataWIFIUp(networkManager.getConnectivityManager());
        boolean bittorrentOnMobileData = !ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_WIFI_ONLY);
        return wifiIsUp || bittorrentOnMobileData;
    }

    private boolean isSeedable() {
        return uiBittorrentDownload.getState() == TransferState.FINISHED;
    }

    private boolean isOpenable() {
        return uiBittorrentDownload.isComplete() && uiBittorrentDownload.getItems().size() > 0;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // R.id.fragment_transfer_detail_menu_delete
        // R.id.fragment_transfer_detail_menu_pause_resume
        MenuItem pauseResumeMenuItem = menu.findItem(R.id.fragment_transfer_detail_menu_pause_resume);
        if (isPausable()) {
            pauseResumeMenuItem.setIcon(R.drawable.action_bar_pause);
        }
        if (isResumable()) {
            // TODO: some sort of Play icon, I guess we use that one below
            pauseResumeMenuItem.setIcon(R.drawable.contextmenu_icon_play_transfer);
        }
        // R.id.fragment_transfer_detail_menu_open
        MenuItem openMenuItem = menu.findItem(R.id.fragment_transfer_detail_menu_open);
        openMenuItem.setVisible(isOpenable());
        // R.id.fragment_transfer_detail_menu_seed
        MenuItem seedMenuItem = menu.findItem(R.id.fragment_transfer_detail_menu_seed);
        seedMenuItem.setVisible(isSeedable());

        MenuItem fiatMenuItem = menu.findItem(R.id.fragment_transfer_detail_menu_donate_fiat);
        MenuItem bitcoinMenuItem = menu.findItem(R.id.fragment_transfer_detail_menu_donate_bitcoin);
        if (!uiBittorrentDownload.hasPaymentOptions()) {
            fiatMenuItem.setVisible(false);
            bitcoinMenuItem.setVisible(false);
        } else {
            PaymentOptions po = uiBittorrentDownload.getPaymentOptions();
            fiatMenuItem.setVisible(po.paypalUrl != null);
            bitcoinMenuItem.setVisible(po.bitcoin != null);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Activity activity = getActivity();
        int itemId = item.getItemId();
        PaymentOptions paymentOptions = uiBittorrentDownload.getPaymentOptions();
        switch (itemId) {
            case R.id.fragment_transfer_detail_menu_delete:
                // TODO: add an action listener and pass to dialog
                new CancelMenuAction(activity, uiBittorrentDownload, true, true).onClick(activity);
                break;
            case R.id.fragment_transfer_detail_menu_pause_resume:
                if (isPausable()) {
                    new PauseDownloadMenuAction(activity, uiBittorrentDownload).onClick(activity);
                } else if (isResumable()) {
                    new ResumeDownloadMenuAction(activity, uiBittorrentDownload, R.string.resume_torrent_menu_action).onClick(activity);
                }
                break;
            case R.id.fragment_transfer_detail_menu_open:
                if (isOpenable()) {
                    String path = uiBittorrentDownload.getSavePath().getAbsolutePath();
                    String mimeType = UIUtils.getMimeType(path);
                    new OpenMenuAction(activity, path, mimeType).onClick(activity);
                }
                break;
            case R.id.fragment_transfer_detail_menu_seed:
                if (isSeedable()) {
                    new SeedAction(activity, uiBittorrentDownload).onClick(activity);
                }
                break;
            case R.id.fragment_transfer_detail_menu_copy_magnet:
                new CopyToClipboardMenuAction(activity,
                        R.drawable.contextmenu_icon_magnet,
                        R.string.transfers_context_menu_copy_magnet,
                        R.string.transfers_context_menu_copy_magnet_copied,
                        uiBittorrentDownload.magnetUri() + BTEngine.getInstance().magnetPeers()
                ).onClick(activity);
                break;
            case R.id.fragment_transfer_detail_menu_copy_infohash:
                new CopyToClipboardMenuAction(activity,
                        R.drawable.contextmenu_icon_copy,
                        R.string.transfers_context_menu_copy_infohash,
                        R.string.transfers_context_menu_copy_infohash_copied,
                        uiBittorrentDownload.getInfoHash()
                ).onClick(activity);
                break;
            case R.id.fragment_transfer_detail_menu_remove_torrent_data:

                break;
            case R.id.fragment_transfer_detail_menu_donate_fiat:
                new SendFiatTipAction(activity, paymentOptions.paypalUrl).onClick(activity);
                break;
            case R.id.fragment_transfer_detail_menu_donate_bitcoin:
                new SendBitcoinTipAction(activity, paymentOptions.bitcoin).onClick(activity);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
