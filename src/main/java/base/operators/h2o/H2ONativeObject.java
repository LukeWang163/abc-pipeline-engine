package base.operators.h2o;

import base.operators.operator.OperatorException;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import water.AutoBuffer;
import water.DKV;
import water.H2O;
import water.Key;
import water.Keyed;

public class H2ONativeObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private static ConcurrentHashMap<Key<?>, Integer> references = new ConcurrentHashMap();
    public final String version;
    public final String key;
    protected volatile byte[] binary;
    private final Object binaryLock = new Object();
    protected volatile List<byte[]> compressedJsons;
    protected volatile List<String> jsonClasses;
    protected final Object jsonLock = new Object();
    protected volatile Keyed<?> h2oObject;
    private final Object h2oObjectLock = new Object();

    public static boolean isReferenced(Key<?> key) {
        return references.containsKey(key);
    }

    public H2ONativeObject(Keyed<?> h2oObject) throws OperatorException {
        this.version = H2O.ABV.projectVersion();
        this.key = h2oObject._key.toString();
        this.h2oObject = h2oObject;
        references.compute(h2oObject._key, (k, referenceCount) -> {
            return referenceCount == null ? 1 : referenceCount + 1;
        });
    }

    public H2ONativeObject(String version, String key, byte[] binary) {
        this.version = version;
        this.key = key;
        this.binary = binary;
    }

    public H2ONativeObject(String version, String key, List<byte[]> compressedJsons, List<String> jsonClasses) {
        this.version = version;
        this.key = key;
        this.compressedJsons = compressedJsons;
        this.jsonClasses = jsonClasses;
    }

    public Keyed<?> getH2OObject() throws OperatorException {
        ClusterManager.startCluster();
        if (this.h2oObject == null) {
            synchronized(this.h2oObjectLock) {
                if (this.h2oObject == null) {
                    if (!H2OUtils.isVersionCompatible(this.version, H2O.ABV.projectVersion())) {
                        throw new OperatorException("H2O model version: " + this.version + " is incompatible with embedded version: " + H2O.ABV.projectVersion());
                    }

                    Key h2oKey = Key.make(this.key);

                    try {
                        references.compute(h2oKey, (k, referenceCount) -> {
                            try {
                                if (referenceCount == null) {
                                    if (this.binary != null) {
                                        this.h2oObject = ClusterManager.deserializeIntoClusterFromBinary(this.binary);
                                    } else {
                                        this.h2oObject = ClusterManager.deserializeIntoClusterFromJson(this.compressedJsons, this.jsonClasses);
                                    }
                                } else {
                                    this.h2oObject = (Keyed)DKV.getGet(h2oKey);
                                }
                            } catch (Exception var5) {
                                throw new RuntimeException(var5);
                            }

                            return referenceCount == null ? 1 : referenceCount + 1;
                        });
                    } catch (RuntimeException var5) {
                        if (var5.getCause() != null && var5.getCause() instanceof OperatorException) {
                            throw (OperatorException)var5.getCause();
                        }

                        throw var5;
                    }
                }
            }
        }

        return this.h2oObject;
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.h2oObject != null) {
            references.compute(this.h2oObject._key, (k, referenceCount) -> {
                if (referenceCount == 1) {
                    ClusterManager.removeFromCluster(new Keyed[]{this.h2oObject});
                }

                return referenceCount > 1 ? referenceCount - 1 : null;
            });
        }

        super.finalize();
    }

    public byte[] getBinary() throws OperatorException {
        if (this.binary == null) {
            synchronized(this.binaryLock) {
                if (this.binary == null) {
                    this.getH2OObject();
                    this.computeBinary();
                }
            }
        }

        return this.binary;
    }

    private void computeBinary() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.h2oObject.writeAll(new AutoBuffer(bos, true)).close();
        this.binary = bos.toByteArray();
    }

    public List<byte[]> getCompressedJsons() throws OperatorException {
        if (this.compressedJsons == null) {
            synchronized(this.jsonLock) {
                if (this.compressedJsons == null) {
                    this.getH2OObject();
                    this.computeCompressedJsonsAndJsonClasses();
                }
            }
        }

        return this.compressedJsons;
    }

    public List<String> getJsonClasses() throws OperatorException {
        if (this.jsonClasses == null) {
            synchronized(this.jsonLock) {
                if (this.jsonClasses == null) {
                    this.getH2OObject();
                    this.computeCompressedJsonsAndJsonClasses();
                }
            }
        }

        return this.jsonClasses;
    }

    protected void computeCompressedJsonsAndJsonClasses() throws OperatorException {
        this.compressedJsons = new LinkedList();
        this.jsonClasses = new LinkedList();

        try {
            String jsonPiece = ClusterManager.toJson(this.h2oObject);
            this.compressedJsons.add(ClusterManager.compress(jsonPiece));
            this.jsonClasses.add(this.h2oObject.getClass().getName());
        } catch (Exception var2) {
            throw new OperatorException("H2O object can't be serialized to JSON", var2);
        }
    }
}
