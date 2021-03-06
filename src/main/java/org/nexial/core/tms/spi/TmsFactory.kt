package org.nexial.core.tms.spi

import org.apache.commons.lang3.StringUtils
import org.nexial.core.NexialConst.TMSSettings.*
import org.nexial.core.tms.TMSOperation
import org.nexial.core.tms.model.TMSAccessData
import org.nexial.core.tms.spi.testrail.APIClient
import org.nexial.core.tms.spi.testrail.TestRailOperations
import org.springframework.context.support.ClassPathXmlApplicationContext

class TmsFactory {
    fun loadTmsData(): TMSAccessData {
        ClassPathXmlApplicationContext("classpath:/nexial-init.xml")
        val username = System.getProperty(TMS_USERNAME)
        val password = System.getProperty(TMS_PASSWORD)
        val url = System.getProperty(TMS_URL)
        val source = System.getProperty(TMS_SOURCE)
        val org = System.getProperty(TMS_ORG)
        return TMSAccessData(source, username, password, url, org)

    }

    fun getClient(data: TMSAccessData, urlSuffix: String = ""): APIClient {
        val url = StringUtils.appendIfMissing(data.url, "/") + urlSuffix
        val client = APIClient(url)
        client.user = data.user
        client.password = data.password
        return client
    }

    fun getTmsInstance(projectId: String): TMSOperation? {
        if (StringUtils.isEmpty(projectId)) return null

        val data = loadTmsData()
        return when (data.source) {
            "testrail" -> {
                //val client = getClient(data)
                TestRailOperations(projectId)
            }
            "azure"    -> {
                // check ready to use valid credentials
                val client = getClient(data, "${data.organisation}/$projectId/_apis/")
                // AzureDevopsOperation(projectId, client)
                null
            }
            else       -> null
        }
    }
}