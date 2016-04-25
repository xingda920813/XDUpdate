# XDUpdate
Android 自动更新/在线参数

# 引入
## 1.导入XDUpdate-x.x.x.jar

## 2.AndroidManifest.xml中添加：
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>    //下载的APK文件存放在大容量存储上
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>      //判断是否是Wifi
    <uses-permission android:name="android.permission.INTERNET"/>                  //连接网络检查更新、下载APK

    <application>
    <service android:name="com.xdandroid.xdupdate.XdUpdateService"/>               //APK下载服务
    </application>
    
# 自动更新
## 1.准备描述更新信息的JSON文件
    {
    "versionCode":4,                            //新版本的versionCode,int型
    "versionName":"1.12",                       //新版本的versionName,String型
    "url":"http://contoso.com/app.apk",         //APK下载地址,String型
    "note":"Bug修复",                           //更新内容,String型
    "md5":"D23788B6A1F95C8B6F7E442D6CA7536C",   //32位MD5值,String型
    "size":17962350                             //大小(字节),int型
    }

## 2.构建XdUpdateAgent对象
    XdUpdateAgent updateAgent = new XdUpdateAgent.Builder()
                    .setJsonUrl("http://contoso.com/update.json") //JSON的URL
                    .setAllow4G(true)                             //是否允许使用运营商网络检查更新(默认不允许)
                    .setIconResId(R.mipmap.ic_launcher)           //设置下载过程中通知栏显示的小图标
                    .build();
     
## 3.检查更新(Activity内)
默认策略下，若用户选择“以后再说”，则**当天**对**该版本**不再提示更新，以防止用户每次打开应用时弹框不胜其烦。适用于入口Activity的自动检查更新。  

    updateAgent.update(this); 
    
强制检查更新，适用于应用“设置”页面的手动检查更新。此方法无视是否允许使用运营商网络和上面的默认策略。     

    updateAgent.forceUpdate(this);   

# 在线参数
## 1.将键值对放到Map中，利用Java序列化，将Map存为文件
建立Java项目，代码如下：

    public static void writeObject(Map<Object,Object> map) throws IOException {
        File file = new File("C:\\Desktop\\map.obj");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(map);
    }

## 2.得到在线参数
    XdOnlineConfig onlineConfig = new XdOnlineConfig.Builder()
                    .setMapUrl("http://contoso.com/map.obj")    //上面生成的文件的URL
                    .setOnConfigAcquiredListener(new XdOnlineConfig.OnConfigAcquiredListener() {
                        public void onConfigAcquired(Map<Object, Object> map) {System.out.println(map);}
                        public void onFailure(Exception e) {e.printStackTrace();}
                    }).build();
    onlineConfig.getOnlineConfig();
