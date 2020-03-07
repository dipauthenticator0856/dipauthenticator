package org.shadowice.flocke.andotp.Services.API;

import org.shadowice.flocke.andotp.Common.Network.Rest.RestClient;

import java.util.ArrayList;

public class BaseList<T> {

    Class<T> m_type = null;
    protected ArrayList<T> m_modelList = null;

    RestClient.ServiceInterface sInterface;

    public RestClient.ServiceInterface getEcombidInterface(){
        sInterface = RestClient.getEcombidService().create(RestClient.ServiceInterface.class);
        return sInterface;
    }

    protected BaseList(Class<T> type) {
        m_type = type;
    }

    public ArrayList<T> getList() {
        if (m_modelList == null)
            m_modelList = new ArrayList<T>();
        return m_modelList;
    }

}
