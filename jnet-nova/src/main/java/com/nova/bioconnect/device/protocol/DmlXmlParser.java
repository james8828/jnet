package com.nova.bioconnect.device.protocol;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * DML XML解析器
 * 处理DML特有格式：<X.field V="..."/>
 * 基于C# XmlNodeReader的解析逻辑
 */
@Component
public class DmlXmlParser {

    private final DocumentBuilderFactory factory;

    public DmlXmlParser() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
    }

    /**
     * 解析XML字符串为Document
     */
    public Document parse(String xml) throws Exception {
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * 获取消息类型（根节点名称）
     */
    public String getMessageType(Document doc) {
        return doc.getDocumentElement().getNodeName();
    }

    /**
     * 获取属性值（V属性）
     */
    public String getAttribute(Element elem, String attrName) {
        return elem != null ? elem.getAttribute(attrName) : "";
    }

    /**
     * 获取V属性值（DML特有格式）
     */
    public String getVAttribute(Element elem) {
        return getAttribute(elem, "V");
    }

    /**
     * 获取子节点的V属性值
     */
    public String getChildVAttribute(Element parent, String childName) {
        Element child = getChildElement(parent, childName);
        return child != null ? getVAttribute(child) : "";
    }

    /**
     * 获取子元素
     */
    public Element getChildElement(Element parent, String childName) {
        if (parent == null) return null;
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                if (elem.getNodeName().equals(childName)) {
                    return elem;
                }
            }
        }
        return null;
    }

    /**
     * 获取元素的文本内容
     */
    public String getTextContent(Element elem) {
        return elem != null ? elem.getTextContent().trim() : "";
    }

    /**
     * 解析HEL消息，提取设备信息
     * 对应C# ProcessHello方法（line 2062-2505）
     */
    public Map<String, String> parseHello(Document doc) {
        Map<String, String> deviceInfo = new HashMap<>();

        Element root = doc.getDocumentElement();

        // 解析HDR部分
        Element hdr = getChildElement(root, "HDR");
        if (hdr != null) {
            deviceInfo.put("control_id", getChildVAttribute(hdr, "HDR.control_id"));
        }

        // 解析DEV部分
        Element dev = getChildElement(root, "DEV");
        if (dev != null) {
            deviceInfo.put("vendor_id", getChildVAttribute(dev, "DEV.vendor_id"));
            deviceInfo.put("device_id", getChildVAttribute(dev, "DEV.device_id"));
            deviceInfo.put("serial_id", getChildVAttribute(dev, "DEV.serial_id"));
            deviceInfo.put("model_id", getChildVAttribute(dev, "DEV.model_id"));
            deviceInfo.put("device_name", getChildVAttribute(dev, "DEV.device_name"));
            deviceInfo.put("manufacturer_name", getChildVAttribute(dev, "DEV.manufacturer_name"));
            deviceInfo.put("hw_version", getChildVAttribute(dev, "DEV.hw_version"));
            deviceInfo.put("sw_version", getChildVAttribute(dev, "DEV.sw_version"));
            deviceInfo.put("facility", getChildVAttribute(dev, "DEV.facility"));
            deviceInfo.put("location", getChildVAttribute(dev, "DEV.location"));

            // 处理device_id中的facility和location（用^分隔）
            Element deviceIdElem = getChildElement(dev, "DEV.device_id");
            if (deviceIdElem != null) {
                String text = getTextContent(deviceIdElem);
                if (text != null && text.contains("^")) {
                    String[] parts = text.split("\\^", 2);
                    if (!deviceInfo.containsKey("facility") || deviceInfo.get("facility").isEmpty()) {
                        deviceInfo.put("facility", parts[0]);
                    }
                    if (parts.length > 1 && (!deviceInfo.containsKey("location") || deviceInfo.get("location").isEmpty())) {
                        deviceInfo.put("location", parts[1]);
                    }
                }
            }

            // 处理sw_version中的语言版本
            Element swVersionElem = getChildElement(dev, "DEV.sw_version");
            if (swVersionElem != null) {
                String text = getTextContent(swVersionElem);
                if (text != null && text.length() > 0) {
                    deviceInfo.put("sw_lang_version", text);
                    // 提取语言代码
                    int lastUnderscore = text.lastIndexOf("_");
                    if (lastUnderscore >= 0) {
                        String lang = text.substring(lastUnderscore + 1);
                        deviceInfo.put("language_long", lang);
                        int dashPos = lang.lastIndexOf("-");
                        if (dashPos > 0) {
                            deviceInfo.put("language_short", lang.substring(0, dashPos));
                        } else {
                            deviceInfo.put("language_short", lang);
                        }
                    } else {
                        deviceInfo.put("language_short", "en");
                        deviceInfo.put("language_long", "en");
                    }
                }
            }
        }

        // 解析DCP部分
        Element dcp = getChildElement(root, "DCP");
        if (dcp != null) {
            Element vendorSpecific = getChildElement(dcp, "DCP.vendor_specific");
            if (vendorSpecific != null) {
                String text = getTextContent(vendorSpecific);
                if (text != null && text.contains("^") && text.contains("=")) {
                    String[] parts = text.split("\\^|=");
                    for (int i = 0; i < parts.length - 1; i += 2) {
                        String key = parts[i].trim();
                        String value = parts[i + 1].trim();
                        switch (key.toLowerCase()) {
                            case "max_op_list_sz":
                                deviceInfo.put("max_op_list_sz", value);
                                break;
                            case "max_pat_list_sz":
                                deviceInfo.put("max_pat_list_sz", value);
                                break;
                            case "mac_address":
                                if (value.length() == 17) {
                                    deviceInfo.put("mac_address", value);
                                }
                                break;
                            case "wifi_mac_address":
                                if (value.length() == 17) {
                                    deviceInfo.put("wifi_mac_address", value);
                                }
                                break;
                        }
                    }
                }
            }
        }

        // 解析DSC部分（描述信息）
        Element dsc = getChildElement(root, "DSC");
        if (dsc != null) {
            deviceInfo.put("max_message_sz", getChildVAttribute(dsc, "DSC.max_message_sz"));

            // 解析topics_supported_cd（支持的主题）
            NodeList topics = dsc.getElementsByTagName("DSC.topics_supported_cd");
            for (int i = 0; i < topics.getLength(); i++) {
                Element topic = (Element) topics.item(i);
                String topicName = getVAttribute(topic);
                deviceInfo.put("topic_" + topicName, "true");
            }

            // 解析directives_supported_cd（支持的指令）
            NodeList directives = dsc.getElementsByTagName("DSC.directives_supported_cd");
            for (int i = 0; i < directives.getLength(); i++) {
                Element directive = (Element) directives.item(i);
                String directiveName = getVAttribute(directive);
                deviceInfo.put("directive_" + directiveName, "true");
            }
        }

        return deviceInfo;
    }

    /**
     * 解析OBS消息，提取观察数据
     * 对应C# ProcessObservation方法
     */
    public Map<String, Object> parseObservation(Document doc) {
        Map<String, Object> obsData = new HashMap<>();
        Element root = doc.getDocumentElement();

        // 获取control_id
        Element hdr = getChildElement(root, "HDR");
        if (hdr != null) {
            obsData.put("control_id", getChildVAttribute(hdr, "HDR.control_id"));
        }

        // 解析SVC节点（可能有多个）
        NodeList svcList = root.getElementsByTagName("SVC");
        for (int i = 0; i < svcList.getLength(); i++) {
            Element svc = (Element) svcList.item(i);
            // 解析每个SVC的数据...
            // 这里简化处理，实际应用中需要详细解析
        }

        return obsData;
    }

    /**
     * 解析EVS消息，提取事件数据
     */
    public Map<String, Object> parseEvents(Document doc) {
        Map<String, Object> eventData = new HashMap<>();
        Element root = doc.getDocumentElement();

        Element hdr = getChildElement(root, "HDR");
        if (hdr != null) {
            eventData.put("control_id", getChildVAttribute(hdr, "HDR.control_id"));
        }

        // 解析EVT节点
        NodeList evtList = root.getElementsByTagName("EVT");
        for (int i = 0; i < evtList.getLength(); i++) {
            Element evt = (Element) evtList.item(i);
            // 解析事件数据...
        }

        return eventData;
    }
}