package dataTypes;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import main.Main;

public class TimeStampedData<E> implements TimeStamped, Serializable
{
	private static final long serialVersionUID = -5631520531456943338L;
	public final E data;
	public final Instant timestamp;
	
	//Constructors
	TimeStampedData(E data)
	{
		this(data,Main.getMain().getClock().instant());
	}
	
	TimeStampedData(E data, Instant time)
	{
		this.data = data;
		this.timestamp = time;
	}
	
	//getters
	E getData() {return data;}
	
    //TimeStamped implementation
    public Instant time() {return timestamp;}
    public long getNano() {return timestamp.getNano();}

    public String getTimeStr()
    {
        DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
		                 .withLocale( Locale.UK )
		                 .withZone( ZoneId.systemDefault() );
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.nnnn");
        return 	"[" +formatter.format( timestamp ) +"] " ;
    }
    
    // general methods
    public String toString() {return getTimeStr() + data.toString();   	
    }
}