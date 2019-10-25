package abc_pipeline_engine.common;

public class Constants {
	public static final String CONFIG_FILE = "idsw.properties";
	public static final String DEV_CONFIG_FILE="idsw-dev.properties";
	public static final String JOB_CONFIG_FILE="job.properties";
	public static final String EXPERIMENT_CONFIG_FILE="experiment.properties";
	public static final String MESSAGE_CONFIG_FILE="messages/dsw/message_en_US.properties";
	public static final String ZH_MESSAGE_CONFIG_FILE="messages/dsw/message_zh_CN.properties";

	public static final String SHARED_PATH = "SHARED_PATH";

	public static final String JUPYTERHUB_URL = "JUPYTERHUB_URL";
	public static final String JUPYTERHUB_API_TOKEN = "jupyterhub_api_token";
	public static final String IDSW_URL = "IDSW_URL";
	

	public static final String ADMIN_USER = "superadmin";
	
	
	public static final String AUTO_SKLEARN_ENDPOINT="AUTO_SKLEARN_ENDPOINT";
	public static final String AUTO_SKLEARN_TEMPLATE_PATH="template/python/auto/";
	public static final String AUTO_SKLEARN_CLASSIFIER="classifier.template";
	public static final String AUTO_SKLEARN_REGRESSOR="regressior.template";
	public static final String AUTO_SKLEARN_SERVICE="service.template";
	
	public static final String POSTGRES_DB_ENDPOINT="POSTGRES_DB_ENDPOINT";

	public static final String KERNEL_SPARKML = "sparkml";
	public static final String KERNEL_TENSORFLOW = "tensorflow";
	public static final String KERNEL_SKLEARN = "sklearn";
	
	// auto-ml
	public static final String IDSW_HDFS_AUTO_PATH="IDSW_HDFS_AUTO_PATH";
	
	// Kerberos
	public static final String KERBEROS_PRINCIPAL = "KERBEROS_PRINCIPAL";
	public static final String KERBEROS_ENABLE = "KERBEROS_ENABLE";
	public static final String KRB5_CONFIG="KRB5_CONFIG";
	public static final String KEYTAB_PATH="KEYTAB_PATH";
	public static final String KEYTAB_DIR="KEYTAB_DIR";

	
	// hdfs
	public static final String HDFS_HA_ENABLE="HDFS_HA_ENABLE";
	public static final String HDFS_HA_CONFIG="HDFS_HA_CONFIG";
	public static final String HDFS_URL = "HDFS_URL";
	public static final String HDFS_HOST="HDFS_HOST";
	public static final String IDSW_HDFS_PATH = "IDSW_HDFS_PATH";
	public static final String EXTRA_HOSTS = "EXTRA_HOSTS";
	public static final String HDFS_CONF_DIR="HDFS_CONF_DIR";
	public static final String HDFS_USER="HDFS_USER";
	
	// deploy
	public static final String VOLUME_DIR = "/data/idsw";
	
	// dataset
	public static final String DATASET_TMP_DIR="dataset.tmp.dir";
	
	// experiment
	public static final String EXPERIMENT_TMP_DIR="experiment.tmp.dir";
	
	//notebook
	public static final String NOTEBOOK_TMP_DIR="notebook.tmp.dir";
	
	// train job
	public static final String TRAIN_JOB_TMP_DIR="trainjob.tmp.dir";
	
	// oozie
	public static final String OOZIE_TMP_DIR="OOZIE_TMP_DIR";
	public static final String SCHEDULE_MODE="idsw.schedule.mode";
	public static final String OPERATION_PYTHON_EXECUTE_PREFIX="operation.python.execute.prefix";
	public static final String OPERATION_JAVA_EXECUTE_PREFIX="operation.java.execute.prefix";
	
	// docker
	public static final String DOCKER_EXPERIMENT_SKLEARN="DOCKER_EXPERIMENT_SKLEARN";
	public static final String DOCKER_DEPLOY_SKLEARN="DOCKER_DEPLOY_SKLEARN";
	public static final String DOCKER_HOST="DOCKER_HOST";
	
	//////////////////////////  HD-Authorization  ///////////////////////////////////
	public static final String HD_ENABLE="HD_ENABLE";
	public static final String HD_ADMIN_URl="HD_ADMIN_URL";
	public static final String ADMIN_KC_REALM="ADMIN_KC_REALM";
	public static final String ADMIN_NAME="ADMIN_NAME";
	public static final String USER_KC_REALM="USER_KC_REALM";

	// experiment cache
	public static final String ENABLE_CACHE="ENABLE_CACHE";
	
}
