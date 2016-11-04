## XDUpdate
#### Android 自动更新/在线参数/阿里云OSS一键上传更新

- 支持Android 7.0，不会因FileUriExposedException而无法安装下载的APK

- JSON、APK、Map文件的URL需要支持外链，即可以被直接访问，可考虑放在Git仓库、OSS或自己的服务器上

![Alt text](https://raw.githubusercontent.com/xingda920813/XDUpdate/master/Screenshot_notification.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/XDUpdate/master/Screenshot_dialog.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/XDUpdate/master/Screenshot_downloading.png)

## 引入

build.gradle中添加

	compile 'com.xdandroid:xdupdate:+'

## 自动更新
#### 1.准备描述更新信息的JSON文件
    {
    "versionCode":4,                          //新版本的versionCode,int型
    "versionName":"1.12",                     //新版本的versionName,String型
    "url":"http://contoso.com/app.apk",       //APK下载地址,String型
    "note":"Bug修复",                         //更新内容,String型
    "md5":"D23788B6A1F95C8B6F7E442D6CA7536C", //32位MD5值,String型
    "size":17962350                           //大小(字节),int型
    }

#### 2.构建XdUpdateAgent对象
    XdUpdateAgent updateAgent = new XdUpdateAgent.Builder()
                .setDebugMode(false)                          //是否显示调试信息(默认:false)
								.setUpdateBean(XdUpdateBean updateBean)				//设置通过其他途径得到的XdUpdateBean
                .setJsonUrl("http://contoso.com/update.json") //JSON文件的URL
                .setAllow4G(true)                             //是否允许使用运营商网络检查更新(默认:false)
                .setShowNotification(true)                    
                //使用通知提示用户有更新，用户点击通知后弹出提示框，而不是检测到更新直接弹框(默认:true，仅对非强制检查更新有效)
                .setOnUpdateListener(new XdUpdateAgent.OnUpdateListener() {
						//取得更新信息JSON后的回调(可选)，回调在主线程，可执行UI操作，updateBean为JSON对应的数据结构  
                        public void onUpdate(boolean needUpdate, XdUpdateBean updateBean) {
                            if (!needUpdate) Toast.makeText(context,"您的应用为最新版本",Toast.LENGTH_SHORT).show();
                        }
                    })
                .setDownloadText("立即下载")                   //可选，默认为左侧所示的文本
                .setInstallText("立即安装(已下载)")
                .setLaterText("以后再说")
                .setHintText("版本更新")
                .setDownloadingText("正在下载")
								.setIconResId(R.mipmap.ic_launcher)           //设置在通知栏显示的通知图标资源ID(可选)
                .build();

#### 3.检查更新
适用于App入口的自动检查更新。默认策略下，若用户选择“以后再说”或者划掉了通知栏的更新提示，则**当天**对**该版本**不再提示更新，防止用户当天每次打开应用时都提示，不胜其烦。  

    updateAgent.update(getActivity());

适用于应用“设置”页面的手动检查更新。此方法无视是否允许使用运营商网络和上面的默认策略，强制检查更新，有更新时直接弹出提示框。     

    updateAgent.forceUpdate(getActivity());   

弹出的更新对话框中只有“立即更新”按钮，没有“以后再说”，且不能取消对话框。用户体验不好，不推荐使用。     

    updateAgent.forceUpdateUncancelable(getActivity());   

为防止内存泄漏，需调用updateAgent.onDestroy().

可通过updateAgent.getDialog()得到更新提示框的AlertDialog.

#### 4.若不想使用JSON文件，可传入由其他途径得到的XdUpdateBean

```
	XdUpdateAgent.Builder.setUpdateBean(XdUpdateBean updateBean);
```

可使用第三方推送服务的自定义消息/透传功能，接收到服务端推送过来的JSON(String)后，解析成一个XdUpdateBean，传入上述方法，即可使用推送带过来的JSON进行更新提示。

注意不是普通消息，这样会直接在通知栏上显示内容，不会进到自定义的代码处理块。

## 阿里云OSS一键上传更新

位于/XdUploadClient/下，XdUpdateClient.jar为程序主体，XdUpdateClient.cmd为Windows下使用的上传脚本，XdUpdateClient.sh为Linux下使用的上传脚本，config.properties为配置文件，其他文件为源码。

一般使用只需把上述 4 个文件放到一个目录（下面称为工作目录）下即可。

#### 1.将更新过的APK命名为 (包名).apk，放到工作目录下

#### 2.编辑config.properties配置文件

```
packageName = com.xdandroid.myproject		//包名
releaseNote = Bug修复		//更新内容
cdnDomain = http://my-project.oss-cn-shenzhen.aliyuncs.com/		//文件URL的主机名部分(斜线后置)
endpoint = http://oss-cn-shenzhen.aliyuncs.com		//OSS的Endpoint(无斜线)
accessKeyId = xXxxxXxXxxXxxxxX		//OSS的AccessKeyId
accessKeySecret = xXxxxxxXXxxXxxxXxxXxxXXXXxxXxx		//OSS的AccessKeySecret
bucketName = my-project		//OSS的BucketName
pathPrefix = download/		//文件URL的路径部分（不含文件名, 斜线后置）
```

上传后的APK安装包的URL为 : cdnDomain + pathPrefix + packageName + ".apk"

上传后的JSON文件的URL为 : cdnDomain + pathPrefix + packageName + ".json"

#### 3.将JSON文件的URL填入XdUpdateAgent.Builder的setJsonUrl(String jsonUrl)

#### 4.运行XdUpdateClient.cmd/XdUpdateClient.sh，等待上传完成

Linux系统下，XdUpdateClient.sh需具有"可执行"文件系统权限。

#### 5.指定使用的配置文件(可选)

运行XdUpdateClient.jar时可以带一个参数，传入配置文件的路径，即可使用该配置文件，而不是默认的config.properties。

(此时，可以再带第 2 个参数，用来 override 掉 properties 中的更新内容。)

```
	java -jar XdUploadClient.jar my-project.properties
```

若不带参数运行XdUpdateClient.jar，将使用与XdUpdateClient.jar同目录下的config.properties。

## 百川 HotFix 结合 LeanCloud Push 推送下发热补丁

位于/PushHotFixAtLeanCloud/下，PushHotFixAtLeanCloud.jar为程序主体，PushHotFixAtLeanCloud.cmd为Windows下使用的推送脚本，PushHotFixAtLeanCloud.sh为Linux下使用的推送脚本，config.properties为配置文件，其他文件为源码。

一般使用只需把上述 3 个文件放到一个目录（下面称为工作目录）下即可。

#### 1.编辑config.properties配置文件

```
action = com.xdandroid.myproject.HOTFIX		//自定义 Receiver 匹配的 Action
versionName = 1.0.0		//App 的 versionName
appId = xxXXXxxXXxxxxxXXxXxxxxXX-xxXxxXxx		//LeanCloud AppId
appKey = XXxxxxxxXxxXXxxxxxXxxXxx		//LeanCloud AppKey
```

#### 2.自定义 Receiver

Manifest:

```
<receiver android:name=".receiver.LeanReceiver">
	<intent-filter>
		<action android:name="android.intent.action.BOOT_COMPLETED"/>
		<action android:name="android.intent.action.USER_PRESENT"/>
		<action android:name="android.net.conn.CONNECTIVITY_CHANGE"/>
		<!-- 自定义 Receiver 匹配的 Action -->
		<action android:name="com.xdandroid.myproject.HOTFIX"/>
  </intent-filter>
</receiver>
```

Push:

```
@JsonObject(fieldDetectionPolicy = JsonObject.FieldDetectionPolicy.NONPRIVATE_FIELDS)
public class Push implements Serializable {

  public PushCustomParams pushCustomParams;
  public String title;
  public String alert;

  @JsonObject(fieldDetectionPolicy = JsonObject.FieldDetectionPolicy.NONPRIVATE_FIELDS)
  public static class PushCustomParams implements Serializable {

    public String deliveryItemId;
    public String type;
    public String subType;
  }
}
```

LeanReceiver:

```
@Override
public void onReceive(Context context, Intent intent) {
	if (intent == null) return;
	if (!"com.xdandroid.myproject.HOTFIX".equals(intent.getAction())) return;
	Bundle extras = intent.getExtras();
	if (extras == null) return;
	String json = extras.getString("com.avos.avoscloud.Data", "");
	Push push = LoganSquare.parse(json, Push.class);
	if (push == null || push.pushCustomParams == null || TextUtils.isEmpty(push.pushCustomParams.type)) return;
	switch (push.pushCustomParams.type) {
		case "hotfix":
			if (XdUpdateUtils.getVersionName(context).equals(push.pushCustomParams.subType))
				HotFixManager.getInstance().queryNewHotPatch();
			break;
		default:
			break;
	}
}
```

#### 3.发布热补丁后，运行 PushHotFixAtLeanCloud.cmd/PushHotFixAtLeanCloud.sh 进行推送

## 在线参数
#### 1.准备参数文件
建立JavaSE项目，先将键值对存放在Map中，然后将Map传入下面的writeObject方法，得到参数文件。

    public static void writeObject(Map<Serializable,Serializable> map) throws IOException {
        File file = new File("C:\\Users\\${user-account-name}\\Desktop\\map.obj");     //指定文件生成路径
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(map);
        oos.close();
    }

#### 2.得到在线参数
    XdOnlineConfig onlineConfig = new XdOnlineConfig.Builder()
                    .setDebugMode(false)                         //是否显示调试信息(默认:false)
                    .setMapUrl("http://contoso.com/map.obj")     //参数文件的URL
                    .setOnConfigAcquiredListener(new XdOnlineConfig.OnConfigAcquiredListener() {
						//主线程回调，可执行UI操作
                        public void onConfigAcquired(Map<Serializable, Serializable> map) {     
                            System.out.println(map);             //成功，传入Map
                        }

                        public void onFailure(Throwable e) {
                            e.printStackTrace();                 //失败，传入Throwable
                        }                           
                    }).build();
    onlineConfig.getOnlineConfig();

为防止内存泄漏，需调用onlineConfig.onDestroy().
