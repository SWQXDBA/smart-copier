package test.competitor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import org.swqxdba.smartconvert.Copier;

public class BeanUtilCopier implements Copier {

    @Override
    public void copy(Object src, Object target) {
        BeanUtil.copyProperties(src, target);
    }

    CopyOptions ignoreNullOptions = CopyOptions.create().setIgnoreNullValue(true);
    @Override
    public void copyNonNullProperties(Object src, Object target) {
        BeanUtil.copyProperties(src, target, ignoreNullOptions);
    }

    CopyOptions mergeOption =  CopyOptions.create().setOverride(false);
    @Override
    public void merge(Object src, Object target) {

        BeanUtil.copyProperties(src, target,mergeOption );
    }
}
