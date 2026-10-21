import com.tide.journal.domain.EtfHistory;
import java.nio.file.*;import java.util.*;
public final class EtfSourceProbe {
 public static void main(String[]args)throws Exception{
  List<EtfHistory.Row> rows=EtfHistory.parse(new String(Files.readAllBytes(Paths.get(args[0])),java.nio.charset.StandardCharsets.UTF_8));
  System.out.println("Parsed "+rows.size()+" days: "+rows.get(0).date+" to "+rows.get(rows.size()-1).date);
 }
}
