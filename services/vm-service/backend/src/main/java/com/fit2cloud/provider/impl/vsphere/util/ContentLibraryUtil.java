package com.fit2cloud.provider.impl.vsphere.util;

import com.fit2cloud.common.platform.credential.impl.VsphereCredential;
import com.fit2cloud.common.provider.impl.vsphere.utils.ClsApiClient;
import com.fit2cloud.common.provider.impl.vsphere.utils.SslUtil;
import com.fit2cloud.common.provider.impl.vsphere.utils.VapiAuthenticationHelper;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.provider.impl.vsphere.entity.VsphereTemplate;
import com.fit2cloud.provider.impl.vsphere.entity.request.VsphereVmBaseRequest;
import com.vmware.content.library.Item;
import com.vmware.content.library.ItemModel;
import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.protocol.HttpConfiguration;
import com.vmware.vim25.VirtualDisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fit2cloud.vm.entity.F2CImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: LiuDi
 * Date: 2022/9/22 2:40 PM
 */
public class ContentLibraryUtil {

    private static Logger logger = LoggerFactory.getLogger(ContentLibraryUtil.class);

    public List<F2CImage> getImages(VsphereVmBaseRequest req) {
        VsphereCredential params = req.getVsphereCredential();
        String server = params.getVCenterIp();
        String userName = params.getVUserName();
        String password = params.getVPassword();
        VapiAuthenticationHelper authHelper = new VapiAuthenticationHelper();
        ArrayList<F2CImage> images = new ArrayList<>();
        try {
            VsphereVmClient vsphereClient = req.getVsphereVmClient();
            logger.info("start to login content library");
            StubConfiguration stubConfiguration = authHelper.loginByUsernameAndPassword(server, userName, password, buildHttpConfiguration());
            logger.info("end login content library");
            ClsApiClient client = new ClsApiClient(authHelper.getStubFactory(), stubConfiguration);
            List<String> libs = client.libraryService().list();
            if (libs.size() <= 0) {
                return null;
            }
            Item itemService = client.itemService();
            for (String lib : libs) {
                List<String> items = itemService.list(lib);
                if (items.size() == 0) {
                    return null;
                }
                for (String itemId : items) {
                    ItemModel item = itemService.get(itemId);
                    if ("ovf".equalsIgnoreCase(item.getType()) || "vm-template".equalsIgnoreCase(item.getType())) {
                        String imageId = item.getLibraryId() + VsphereTemplate.SEPARATOR + item.getId();
                        String imageName = item.getName() + "[内容库]";

                        List<VirtualDisk> diskList = VsphereUtil.getTemplateDisks(vsphereClient, item.getName());
                        F2CImage image = new F2CImage(imageId, imageName, item.getDescription(), null, "ContentLibraries", VsphereUtil.getTemplateDiskSizeInGB(diskList), null)
                                .setDiskInfos(JsonUtil.toJSONString(diskList));
                        images.add(image);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            authHelper.logout();
        }
        return images;
    }

    public HttpConfiguration buildHttpConfiguration() {
        return (new HttpConfiguration.Builder()).setSslConfiguration(buildSslConfiguration()).getConfig();
    }

    private HttpConfiguration.SslConfiguration buildSslConfiguration() {
        HttpConfiguration.SslConfiguration sslConfig;
        SslUtil.trustAllHttpsCertificates();
        sslConfig = (new HttpConfiguration.SslConfiguration.Builder()).disableCertificateValidation().disableHostnameVerification().getConfig();
        return sslConfig;
    }

}
