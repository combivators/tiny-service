package net.tiny.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

public class MapEntry<K, V> {

    private List<KeyValue<K, V>> entry = new ArrayList<KeyValue<K, V>>();

    public MapEntry() {}

    public MapEntry(Map<K, V> map) {
        for (Map.Entry<K, V> e : map.entrySet()) {
            entry.add(new KeyValue<K, V>(e));
        }
    }

    @XmlElement
    public List<KeyValue<K, V>> getEntry() {
        return entry;
    }

    public void setEntry(List<KeyValue<K, V>> entry) {
        this.entry = entry;
    }
}
