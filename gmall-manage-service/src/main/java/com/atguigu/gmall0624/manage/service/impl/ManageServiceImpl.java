package com.atguigu.gmall0624.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.*;
import com.atguigu.gmall0624.config.RedistUtil;
import com.atguigu.gmall0624.constant.ManageConst;
import com.atguigu.gmall0624.manage.mapper.*;
import com.atguigu.gmall0624.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private  SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private RedistUtil redistUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        //select * from baseCatalog2 where catalog2()
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    //调用服务层获取数据
    @Override
    public List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo) {
        return baseAttrInfoMapper.select(baseAttrInfo);

    }

    /**
     * 保存平台属性
     */
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //修改 baseAttrInfo
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            //保存
            //baseAttrInfo代表页面传过来的数据
            //分别插入到两张表baseAttrInfo , baseAttrValue
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //baseAttrValue 修改 （先删除原有数据，再添加所有的数据）
        //delete * from baseAttrValue where attrId = ? baseAttrInfo.getId()
        BaseAttrValue baseAttrValue1Del = new BaseAttrValue();
        baseAttrValue1Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue1Del);
        System.out.println("delete数据");


        System.out.println("插入之后：" + baseAttrInfo.getId());
        //baseAttrValue 、接收baseAttrValue的集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //保存数据 value ，attrId =baseAttrInfo.getId()
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }

    }

    /**
     * 通过attrid查询平台属性集合
     *
     * @param attrId
     * @return
     */
    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //select * from baseInfo where id = attrId
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //赋值
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        return null;
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }


    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        //spuInfo 表示从前台页面传过来的数据
        spuInfoMapper.insertSelective(spuInfo);
        //spuImg
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        //spuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                //spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
//               if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
//                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
//                       spuSaleAttrValue.setSpuId(spuInfo.getId());
//                       spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
//                   }
//               }
                if (ckeckListIsEmpty(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    //根据属性查找图片集合
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }


    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {

        skuInfoMapper.insertSelective(skuInfo);

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (ckeckListIsEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (ckeckListIsEmpty(skuSaleAttrValueList)){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

        if (ckeckListIsEmpty(skuInfo.getSkuImageList())){
            for (SkuImage skuImage : skuInfo.getSkuImageList()) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //return getSkuInfoRedisSet(skuId);
        SkuInfo skuInfo = new SkuInfo();
        Jedis jedis = null;
        //测试工具类
        try {
            //获取jedis
            jedis = redistUtil.getJedis();
            //定义key
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            //获取缓存数据
            String skuJson = jedis.get(skuKey);
            //什么时候上锁
            if (skuJson == null){
                //获取数据库数据并放入缓存
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.93.225:6379");
                //获取redisson
                RedissonClient redisson = Redisson.create(config);

                RLock lock = redisson.getLock("my-lock");

                //lock.lock(10, TimeUnit.SECONDS);
                boolean res = lock.tryLock(100,10,TimeUnit.SECONDS);
                if (res){
                    try {
                        //走数据库DB
                        skuInfo = getSkuInfoDB(skuId);
                        //设置过期时间
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    }finally{
                        lock.unlock();
                    }
                }
            }else{
                //有缓存，就获取缓存数据
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            //如何解决空指针问题
            if (jedis!=null){
                //关闭
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);

    }

    //redis-set 命令
    private SkuInfo getSkuInfoRedisSet(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        Jedis jedis = null;
        //测试工具类
        try {
            //获取jedis
            jedis = redistUtil.getJedis();
            //定义key
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            //获取缓存数据
            String skuJson = jedis.get(skuKey);
            //什么时候上锁
            if (skuJson == null){
                //说明缓存中没有数据
                //查询数据库 上锁
                //定义一个key
                String skuLockKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
                //锁的值
                String token = UUID.randomUUID().toString().replace("-","");
                String lockKey = jedis.set(skuLockKey, token, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    //查询数据库并放入缓存
                    skuInfo = getSkuInfoDB(skuId);
                    //设置过期时间
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

                    //解锁jedis.del(lockKey);
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));
                    return skuInfo;
                }else {
                    Thread.sleep(1000);

                    //调用方法
                    return getSkuInfo(skuId);
                }
            }else{
                //有缓存，就获取缓存数据
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            //如何解决空指针问题
            if (jedis!=null){
                //关闭
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    //获取数据库中的数据
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo =  skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        //没有查询平台属性集合
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {

        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        //baseAttrInfoList集合装成字符串
        String attrValueIds = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ",");

        System.out.println(attrValueIds);
        return baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
    }

    //判断集合是否为空

    public boolean ckeckListIsEmpty(List list) {
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }

}


