package org.telegram.messenger;

public class ApplicationLoaderImpl extends ApplicationLoader {

    @Override
    protected String onGetApplicationId() {
        return "org.nayagram.platform";
    }
}
